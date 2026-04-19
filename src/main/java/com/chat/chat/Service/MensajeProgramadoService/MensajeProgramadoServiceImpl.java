package com.chat.chat.Service.MensajeProgramadoService;

import com.chat.chat.DTO.AdminDirectMessageScheduledRequestDTO;
import com.chat.chat.DTO.AdminDirectMessagePayloadDTO;
import com.chat.chat.DTO.BulkEmailRequestDTO;
import com.chat.chat.DTO.EmailAttachmentDTO;
import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.DTO.MensajeProgramadoDTO;
import com.chat.chat.DTO.ProgramarMensajeItemDTO;
import com.chat.chat.DTO.ProgramarMensajeRequestDTO;
import com.chat.chat.DTO.ProgramarMensajeResponseDTO;
import com.chat.chat.DTO.ScheduledAttachmentMetaDTO;
import com.chat.chat.DTO.ScheduledBatchResponseDTO;
import com.chat.chat.DTO.ScheduledRecipientUserDTO;
import com.chat.chat.Entity.ChatEntity;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.MensajeProgramadoEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.ChatCerradoException;
import com.chat.chat.Exceptions.E2EGroupValidationException;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Exceptions.ValidacionPayloadException;
import com.chat.chat.Mapper.MensajeProgramadoMapper;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.ChatRepository;
import com.chat.chat.Repository.MensajeProgramadoRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.EmailService.EmailService;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.E2EDiagnosticUtils;
import com.chat.chat.Utils.E2EPayloadUtils;
import com.chat.chat.Utils.EstadoMensajeProgramado;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.MessageType;
import com.chat.chat.Utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class MensajeProgramadoServiceImpl implements MensajeProgramadoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MensajeProgramadoServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MODE_PASSTHROUGH_E2E = "PASSTHROUGH_E2E";
    private static final String MODE_LEGACY_ENCRYPT = "LEGACY_ENCRYPT";
    private static final String TYPE_E2E = "E2E";
    private static final String TYPE_E2E_GROUP = "E2E_GROUP";
    private static final String TYPE_E2E_FILE = "E2E_FILE";
    private static final String TYPE_E2E_GROUP_FILE = "E2E_GROUP_FILE";
    private static final String SCHEDULED_ATTACHMENTS_DIR = "scheduled-email-attachments";
    private static final long DEFAULT_ADMIN_DIRECT_EXPIRES_AFTER_READ_SECONDS = 60L;
    private static final ZoneId SCHEDULED_LOCAL_ZONE = ZoneId.of("Europe/Madrid");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> ALLOWED_ATTACHMENT_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "image/png",
            "image/jpeg"
    );
    private static final int MAX_SCHEDULED_SUBJECT_LENGTH = 255;
    private static final int MAX_SCHEDULED_BODY_LENGTH = 20000;
    private static final int MAX_SCHEDULED_MESSAGE_LENGTH = 4000;
    private static final int MAX_SCHEDULED_RECIPIENTS = 500;
    private static final int MAX_SCHEDULED_ATTACHMENTS = 5;

    private final SecurityUtils securityUtils;
    private final UsuarioRepository usuarioRepository;
    private final ChatRepository chatRepository;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final MensajeProgramadoRepository mensajeProgramadoRepository;
    private final MensajeRepository mensajeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final CifradorE2EMensajeProgramadoService cifradorE2EMensajeProgramadoService;
    private final MensajeProgramadoMapper mensajeProgramadoMapper;
    private final EmailService emailService;

    @Value("${app.chat.scheduled.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.uploads.security.max-file-bytes:26214400}")
    private long maxUploadFileBytes;

    @Value(Constantes.PROP_UPLOADS_ROOT)
    private String uploadsRoot;

    public MensajeProgramadoServiceImpl(SecurityUtils securityUtils,
                                        UsuarioRepository usuarioRepository,
                                        ChatRepository chatRepository,
                                        ChatIndividualRepository chatIndividualRepository,
                                        ChatGrupalRepository chatGrupalRepository,
                                        MensajeProgramadoRepository mensajeProgramadoRepository,
                                        MensajeRepository mensajeRepository,
                                        SimpMessagingTemplate messagingTemplate,
                                        JdbcTemplate jdbcTemplate,
                                        CifradorE2EMensajeProgramadoService cifradorE2EMensajeProgramadoService,
                                        MensajeProgramadoMapper mensajeProgramadoMapper,
                                        EmailService emailService) {
        this.securityUtils = securityUtils;
        this.usuarioRepository = usuarioRepository;
        this.chatRepository = chatRepository;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.mensajeProgramadoRepository = mensajeProgramadoRepository;
        this.mensajeRepository = mensajeRepository;
        this.messagingTemplate = messagingTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.cifradorE2EMensajeProgramadoService = cifradorE2EMensajeProgramadoService;
        this.mensajeProgramadoMapper = mensajeProgramadoMapper;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public ProgramarMensajeResponseDTO crearMensajesProgramados(ProgramarMensajeRequestDTO request) {
        List<String> detalleCampos = new ArrayList<>();
        if (request == null) {
            throw new ValidacionPayloadException("payload requerido", List.of("body"));
        }
        Long authUserId = securityUtils.getAuthenticatedUserId();
        if (request.getCreatedBy() != null && !Objects.equals(request.getCreatedBy(), authUserId)) {
            throw new AccessDeniedException("createdBy no coincide con el usuario autenticado");
        }
        String contenidoRaw = request.getContenido();
        boolean hasContenidoE2E = contenidoRaw != null && !contenidoRaw.trim().isBlank();
        ValidatedE2EPayload validatedE2EPayload = null;
        String message = request.getMessage() == null ? null : request.getMessage().trim();
        String tipoProgramado = Constantes.TIPO_TEXT;
        if (hasContenidoE2E) {
            validatedE2EPayload = validarPayloadE2EProgramadoOrThrow(
                    contenidoRaw,
                    authUserId,
                    request.getChatIds(),
                    request.getScheduledAt());
            if (validatedE2EPayload != null && isFilePayloadType(validatedE2EPayload.type())) {
                tipoProgramado = Constantes.TIPO_FILE;
            }
        } else if (message == null || message.isBlank()) {
            detalleCampos.add("message/contenido");
        }
        if (request.getScheduledAt() == null) {
            detalleCampos.add("scheduledAt/fechaProgramada");
        }
        Instant now = Instant.now();
        if (request.getScheduledAt() != null && !request.getScheduledAt().isAfter(now)) {
            detalleCampos.add("scheduledAt debe ser futuro UTC");
        }
        if (request.getChatIds() == null || request.getChatIds().isEmpty()) {
            detalleCampos.add("chatIds/chatId");
        }
        if (!detalleCampos.isEmpty()) {
            throw new ValidacionPayloadException("Payload invalido para programar mensaje", detalleCampos);
        }

        UsuarioEntity creador = usuarioRepository.findById(authUserId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        Set<Long> chatIdsUnicos = request.getChatIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (chatIdsUnicos.isEmpty()) {
            throw new IllegalArgumentException("chatIds no contiene ids validos");
        }

        String batchId = UUID.randomUUID().toString();
        List<MensajeProgramadoEntity> aGuardar = new ArrayList<>();
        List<ProgramarMensajeItemDTO> items = new ArrayList<>();
        for (Long chatId : chatIdsUnicos) {
            ChatEntity chat = validarPermisoSobreChat(chatId, authUserId);
            if (validatedE2EPayload != null) {
                validarCompatibilidadTipoE2EConChat(
                        validatedE2EPayload.type(),
                        chat,
                        authUserId,
                        chatIdsUnicos,
                        request.getScheduledAt());
            }
            MensajeProgramadoEntity entity = new MensajeProgramadoEntity();
            entity.setCreatedBy(creador);
            entity.setChat(chat);
            entity.setMessageContent(hasContenidoE2E ? contenidoRaw : message);
            entity.setScheduledAt(request.getScheduledAt());
            entity.setStatus(EstadoMensajeProgramado.PENDING);
            entity.setAttempts(0);
            entity.setLastError(null);
            entity.setScheduledBatchId(batchId);
            entity.setWsEmitted(false);
            entity.setWsEmittedAt(null);
            entity.setWsDestinations(null);
            entity.setWsEmitError(null);
            entity.setPersistedMessageId(null);
            aGuardar.add(entity);
        }

        List<MensajeProgramadoEntity> guardados = mensajeProgramadoRepository.saveAll(aGuardar);
        LOGGER.info("[SCHEDULED_MESSAGE_CREATE] userId={} chatIds={} scheduledAtUTC={} cantidadItemsCreados={} batchId={} hasContenidoE2E={} tipo={}",
                authUserId,
                chatIdsUnicos,
                request.getScheduledAt(),
                guardados.size(),
                batchId,
                hasContenidoE2E,
                tipoProgramado);
        for (MensajeProgramadoEntity guardado : guardados) {
            items.add(mensajeProgramadoMapper.toProgramarMensajeItemDto(guardado));
        }

        ProgramarMensajeResponseDTO out = new ProgramarMensajeResponseDTO();
        out.setOk(true);
        out.setScheduledBatchId(batchId);
        out.setItems(items);
        return out;
    }

    @Override
    @Transactional
    public ScheduledBatchResponseDTO crearMensajesDirectosAdminProgramados(AdminDirectMessageScheduledRequestDTO request) {
        validateAdminOnly();
        if (request == null) {
            throw new ValidacionPayloadException("payload requerido", List.of("body"));
        }

        LOGGER.info("[SCHEDULED_ADMIN_DIRECT_CREATE_REQUEST] authUserId={} audienceMode={} userIds={} scheduledAtUTC={} scheduledAtLocal={} nowUTC={}",
                securityUtils.getAuthenticatedUserId(),
                request.getAudienceMode(),
                request.getUserIds(),
                request.getScheduledAt(),
                request.getScheduledAtLocal(),
                Instant.now());
        Instant scheduledAt = resolveAndValidateScheduledAt(request.getScheduledAt(), request.getScheduledAtLocal(), "ADMIN_DIRECT");
        validateScheduledAdminDirectRequest(request, scheduledAt);
        List<String> errors = new ArrayList<>();
        List<UsuarioEntity> destinatarios = resolveAdminAudienceUsers(request.getAudienceMode(), request.getUserIds());
        if (destinatarios.isEmpty()) {
            errors.add("userIds/audienceMode");
        }
        Map<Long, AdminDirectMessagePayloadDTO> payloadsByUserId = validateAndMapScheduledAdminPayloads(
                request.getEncryptedPayloads(),
                destinatarios,
                scheduledAt,
                errors);
        if (!errors.isEmpty()) {
            throw new ValidacionPayloadException("Payload invalido para programar mensaje administrativo", errors);
        }

        Long adminId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));
        long expiresAfterReadSeconds = normalizeAdminDirectExpiry(request.getExpiresAfterReadSeconds());
        List<ScheduledRecipientUserDTO> recipientUsers = buildRecipientUsers(destinatarios);
        boolean encryptedFlow = hasEncryptedAdminPayloads(request.getEncryptedPayloads());
        String legacyPlainContent = encryptedFlow ? null : resolveLegacyAdminDirectContent(request);
        String adminPayload = serializeAdminPayload(new AdminScheduledPayload(
                "DIRECT_MESSAGE",
                normalizeAudienceMode(request.getAudienceMode()),
                encryptedFlow ? null : sanitizePlainText(request.getMessage(), MAX_SCHEDULED_MESSAGE_LENGTH),
                null,
                null,
                collectUserIds(recipientUsers),
                collectRecipientEmails(recipientUsers),
                recipientUsers,
                List.of(),
                expiresAfterReadSeconds));

        String batchId = UUID.randomUUID().toString();
        List<MensajeProgramadoEntity> rows = new ArrayList<>();
        for (UsuarioEntity destinatario : destinatarios) {
            AdminDirectMessagePayloadDTO payload = payloadsByUserId.get(destinatario.getId());
            ChatIndividualEntity chat = resolveOrCreateAdminDirectChat(admin,
                    destinatario,
                    payload == null ? null : payload.getChatId());
            MensajeProgramadoEntity row = new MensajeProgramadoEntity();
            row.setCreatedBy(admin);
            row.setChat(chat);
            row.setMessageContent(payload == null ? legacyPlainContent : payload.getContenido());
            row.setScheduledAt(scheduledAt);
            row.setStatus(EstadoMensajeProgramado.PENDING);
            row.setAttempts(0);
            row.setLastError(null);
            row.setScheduledBatchId(batchId);
            row.setDeliveryType(Constantes.SCHEDULED_DELIVERY_TYPE_CHAT_MESSAGE);
            row.setAdminMessage(true);
            row.setMessageTemporal(true);
            row.setExpiresAfterReadSeconds(expiresAfterReadSeconds);
            row.setAdminPayload(adminPayload);
            rows.add(row);
        }

        List<MensajeProgramadoEntity> saved = mensajeProgramadoRepository.saveAll(rows);
        LOGGER.info("[SCHEDULED_ADMIN_DIRECT_CREATE] adminId={} recipients={} scheduledAtUTC={} batchId={}",
                adminId,
                saved.size(),
                scheduledAt,
                batchId);
        for (MensajeProgramadoEntity row : saved) {
            LOGGER.info("[SCHEDULED_ADMIN_DIRECT_ITEM_CREATED] scheduledId={} chatId={} scheduledAtUTC={} status={} deliveryType={} adminMessage={} messageTemporal={} expiresAfterReadSeconds={}",
                    row.getId(),
                    row.getChat() == null ? null : row.getChat().getId(),
                    row.getScheduledAt(),
                    row.getStatus(),
                    row.getDeliveryType(),
                    row.isAdminMessage(),
                    row.isMessageTemporal(),
                    row.getExpiresAfterReadSeconds());
        }
        return buildScheduledBatchResponse(saved);
    }

    @Override
    @Transactional
    public ScheduledBatchResponseDTO crearBulkEmailsProgramados(BulkEmailRequestDTO request, List<MultipartFile> attachments) {
        validateAdminOnly();
        Instant scheduledAt = resolveAndValidateScheduledAt(
                request == null ? null : request.getScheduledAt(),
                request == null ? null : request.getScheduledAtLocal(),
                "ADMIN_BULK_EMAIL");
        validateScheduledBulkEmailRequest(request, attachments);

        LOGGER.info("[SCHEDULED_ADMIN_BULK_EMAIL_CREATE_REQUEST] authUserId={} audienceMode={} userIds={} recipientEmails={} scheduledAtUTC={} scheduledAtLocal={} attachmentCountDeclared={} attachmentCountReceived={} nowUTC={}",
                securityUtils.getAuthenticatedUserId(),
                request.getAudienceMode(),
                request.getUserIds(),
                request.getRecipientEmails(),
                request.getScheduledAt(),
                request.getScheduledAtLocal(),
                request.getAttachmentCount(),
                attachments == null ? 0 : attachments.size(),
                Instant.now());
        List<ScheduledEmailRecipient> destinatarios = resolveScheduledEmailRecipients(request);
        if (destinatarios.isEmpty()) {
            throw new ValidacionPayloadException("Payload invalido para programar bulk email", List.of("recipientEmails/userIds"));
        }

        Long adminId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));
        String batchId = UUID.randomUUID().toString();
        String attachmentPayload = persistScheduledAttachments(batchId, attachments);
        List<ScheduledEmailRecipient> destinatariosSeguros = normalizeScheduledRecipients(destinatarios);
        List<ScheduledRecipientUserDTO> recipientUsers = buildRecipientUsersFromScheduledRecipients(destinatariosSeguros);
        List<ScheduledAttachmentMetaDTO> attachmentsMeta = readAttachmentMeta(attachmentPayload);
        String adminPayload = serializeAdminPayload(new AdminScheduledPayload(
                "BULK_EMAIL",
                normalizeAudienceMode(request.getAudienceMode()),
                null,
                sanitizePlainText(request.getSubject(), MAX_SCHEDULED_SUBJECT_LENGTH),
                sanitizeBodyText(request.getBody(), MAX_SCHEDULED_BODY_LENGTH),
                collectUserIds(recipientUsers),
                collectRecipientEmails(recipientUsers),
                recipientUsers,
                attachmentsMeta,
                null));

        List<MensajeProgramadoEntity> rows = new ArrayList<>();
        for (ScheduledEmailRecipient destinatario : destinatariosSeguros) {
            MensajeProgramadoEntity row = new MensajeProgramadoEntity();
            row.setCreatedBy(admin);
            row.setChat(null);
            row.setMessageContent(request.getBody().trim());
            row.setScheduledAt(scheduledAt);
            row.setStatus(EstadoMensajeProgramado.PENDING);
            row.setAttempts(0);
            row.setLastError(null);
            row.setScheduledBatchId(batchId);
            row.setDeliveryType(Constantes.SCHEDULED_DELIVERY_TYPE_ADMIN_BULK_EMAIL);
            row.setRecipientEmail(destinatario.email());
            row.setEmailSubject(request.getSubject().trim());
            row.setAttachmentPayload(attachmentPayload);
            row.setAdminPayload(adminPayload);
            rows.add(row);
        }

        List<MensajeProgramadoEntity> saved = mensajeProgramadoRepository.saveAll(rows);
        LOGGER.info("[SCHEDULED_ADMIN_BULK_EMAIL_CREATE] adminId={} recipients={} scheduledAtUTC={} batchId={} attachments={}",
                adminId,
                saved.size(),
                scheduledAt,
                batchId,
                attachments == null ? 0 : attachments.size());
        for (MensajeProgramadoEntity row : saved) {
            LOGGER.info("[SCHEDULED_ADMIN_BULK_EMAIL_ITEM_CREATED] scheduledId={} recipientEmail={} scheduledAtUTC={} status={} deliveryType={} attachmentPayloadPresent={}",
                    row.getId(),
                    row.getRecipientEmail(),
                    row.getScheduledAt(),
                    row.getStatus(),
                    row.getDeliveryType(),
                    row.getAttachmentPayload() != null && !row.getAttachmentPayload().isBlank());
        }
        return buildScheduledBatchResponse(saved);
    }

    private ScheduledBatchResponseDTO buildScheduledBatchResponse(List<MensajeProgramadoEntity> saved) {
        ScheduledBatchResponseDTO response = new ScheduledBatchResponseDTO();
        response.setOk(true);
        response.setScheduledBatchId(saved == null || saved.isEmpty() ? null : saved.get(0).getId());
        response.setMessage("Scheduled successfully");
        return response;
    }

    private Instant validateScheduledAt(Instant scheduledAt) {
        if (scheduledAt == null) {
            throw new ValidacionPayloadException("Payload invalido para programacion", List.of("scheduledAt/fechaProgramada"));
        }
        if (!scheduledAt.isAfter(Instant.now())) {
            throw new ValidacionPayloadException("Payload invalido para programacion", List.of("scheduledAt debe ser futuro UTC"));
        }
        return scheduledAt;
    }

    private Instant resolveAndValidateScheduledAt(Instant scheduledAtUtc, String scheduledAtLocal, String flow) {
        Instant resolved = scheduledAtUtc;
        if (StringUtils.hasText(scheduledAtLocal)) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(scheduledAtLocal.trim());
                Instant localInstant = localDateTime.atZone(SCHEDULED_LOCAL_ZONE).toInstant();
                if (scheduledAtUtc != null) {
                    long driftSeconds = Math.abs(localInstant.getEpochSecond() - scheduledAtUtc.getEpochSecond());
                    LOGGER.info("[SCHEDULED_AT_RESOLVE] flow={} scheduledAtUTC={} scheduledAtLocal={} localZone={} resolvedFromLocalUTC={} driftSeconds={}",
                            flow,
                            scheduledAtUtc,
                            scheduledAtLocal,
                            SCHEDULED_LOCAL_ZONE,
                            localInstant,
                            driftSeconds);
                } else {
                    LOGGER.info("[SCHEDULED_AT_RESOLVE] flow={} scheduledAtUTC=null scheduledAtLocal={} localZone={} resolvedFromLocalUTC={} driftSeconds=null",
                            flow,
                            scheduledAtLocal,
                            SCHEDULED_LOCAL_ZONE,
                            localInstant);
                }
                resolved = localInstant;
            } catch (Exception ex) {
                LOGGER.warn("[SCHEDULED_AT_RESOLVE_WARN] flow={} scheduledAtUTC={} scheduledAtLocal={} localZone={} errorType={} message={}",
                        flow,
                        scheduledAtUtc,
                        scheduledAtLocal,
                        SCHEDULED_LOCAL_ZONE,
                        ex.getClass().getSimpleName(),
                        ex.getMessage());
            }
        }
        return validateScheduledAt(resolved);
    }

    private void validateAdminOnly() {
        if (securityUtils.hasRole(Constantes.ADMIN) || securityUtils.hasRole(Constantes.ROLE_ADMIN)) {
            return;
        }
        Long requesterId = securityUtils.getAuthenticatedUserId();
        boolean isAdmin = usuarioRepository.findById(requesterId)
                .map(this::isAdminUser)
                .orElse(false);
        if (!isAdmin) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }
    }

    private boolean isAdminUser(UsuarioEntity usuario) {
        if (usuario == null || usuario.getRoles() == null) {
            return false;
        }
        return usuario.getRoles().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(role -> Constantes.ADMIN.equalsIgnoreCase(role) || Constantes.ROLE_ADMIN.equalsIgnoreCase(role));
    }

    private List<UsuarioEntity> resolveAdminAudienceUsers(String audienceMode, List<Long> userIds) {
        Long adminId = securityUtils.getAuthenticatedUserId();
        String mode = audienceMode == null ? "" : audienceMode.trim().toLowerCase();
        if ("all".equals(mode)) {
            return usuarioRepository.findByActivoTrueAndIdNot(adminId).stream()
                    .filter(Objects::nonNull)
                    .filter(UsuarioEntity::isActivo)
                    .filter(u -> !isAdminUser(u))
                    .toList();
        }
        if (!"selected".equals(mode)) {
            throw new ValidacionPayloadException("Payload invalido para programacion", List.of("audienceMode debe ser all o selected"));
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (userIds != null) {
            userIds.stream().filter(Objects::nonNull).forEach(ids::add);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<UsuarioEntity> users = new ArrayList<>();
        for (Long userId : ids) {
            if (Objects.equals(userId, adminId)) {
                continue;
            }
            UsuarioEntity user = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_EXISTE_ID + userId));
            if (!user.isActivo()) {
                continue;
            }
            if (isAdminUser(user)) {
                continue;
            }
            users.add(user);
        }
        return users;
    }

    private Map<Long, AdminDirectMessagePayloadDTO> validateAndMapScheduledAdminPayloads(List<AdminDirectMessagePayloadDTO> encryptedPayloads,
                                                                                          List<UsuarioEntity> destinatarios,
                                                                                          Instant scheduledAt,
                                                                                          List<String> errors) {
        if (encryptedPayloads == null || encryptedPayloads.isEmpty()) {
            return Map.of();
        }

        Long authUserId = securityUtils.getAuthenticatedUserId();
        Set<Long> destinatarioIds = destinatarios == null
                ? Set.of()
                : destinatarios.stream()
                .filter(Objects::nonNull)
                .map(UsuarioEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, AdminDirectMessagePayloadDTO> payloadsByUserId = new java.util.LinkedHashMap<>();
        for (AdminDirectMessagePayloadDTO payload : encryptedPayloads) {
            if (payload == null) {
                errors.add("encryptedPayloads contiene item null");
                continue;
            }
            if (payload.getUserId() == null) {
                errors.add("encryptedPayloads.userId es obligatorio");
                continue;
            }
            if (!StringUtils.hasText(payload.getContenido())) {
                errors.add("encryptedPayloads.contenido es obligatorio para userId=" + payload.getUserId());
                continue;
            }
            if (payloadsByUserId.putIfAbsent(payload.getUserId(), payload) != null) {
                errors.add("encryptedPayloads duplicado para userId=" + payload.getUserId());
                continue;
            }

            ValidatedE2EPayload validated = validarPayloadE2EProgramadoOrThrow(
                    payload.getContenido(),
                    authUserId,
                    payload.getChatId() == null ? List.of() : List.of(payload.getChatId()),
                    scheduledAt);

            if (!destinatarioIds.contains(payload.getUserId())) {
                errors.add("encryptedPayloads contiene userId no incluido en userIds/audienceMode: " + payload.getUserId());
                continue;
            }
            if (validated != null && payload.getChatId() != null) {
                ChatEntity chat = chatRepository.findById(payload.getChatId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + payload.getChatId()));
                validarCompatibilidadTipoE2EConChat(
                        validated.type(),
                        chat,
                        authUserId,
                        Set.of(payload.getChatId()),
                        scheduledAt);
            }
        }

        if (!payloadsByUserId.isEmpty()) {
            for (Long destinatarioId : destinatarioIds) {
                if (!payloadsByUserId.containsKey(destinatarioId)) {
                    errors.add("encryptedPayloads faltante para userId=" + destinatarioId);
                }
            }
        }
        return payloadsByUserId;
    }

    private ChatIndividualEntity resolveOrCreateAdminDirectChat(UsuarioEntity admin, UsuarioEntity receptor) {
        return resolveOrCreateAdminDirectChat(admin, receptor, null);
    }

    private ChatIndividualEntity resolveOrCreateAdminDirectChat(UsuarioEntity admin,
                                                                UsuarioEntity receptor,
                                                                Long requestedChatId) {
        if (admin == null || admin.getId() == null || receptor == null || receptor.getId() == null) {
            throw new IllegalArgumentException("participantes invalidos para chat admin directo");
        }
        if (requestedChatId != null) {
            ChatIndividualEntity chat = chatIndividualRepository.findById(requestedChatId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_INDIVIDUAL_NO_ENCONTRADO));
            boolean samePair = (Objects.equals(chat.getUsuario1().getId(), admin.getId())
                    && Objects.equals(chat.getUsuario2().getId(), receptor.getId()))
                    || (Objects.equals(chat.getUsuario1().getId(), receptor.getId())
                    && Objects.equals(chat.getUsuario2().getId(), admin.getId()));
            if (!samePair) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            if (!chat.isAdminDirect()) {
                chat.setAdminDirect(true);
                return chatIndividualRepository.save(chat);
            }
            return chat;
        }
        return chatIndividualRepository.findAdminDirectChatBetween(admin.getId(), receptor.getId())
                .orElseGet(() -> {
                    ChatIndividualEntity chat = new ChatIndividualEntity();
                    chat.setUsuario1(admin);
                    chat.setUsuario2(receptor);
                    chat.setAdminDirect(true);
                    return chatIndividualRepository.save(chat);
                });
    }

    private void validateScheduledAdminDirectRequest(AdminDirectMessageScheduledRequestDTO request, Instant scheduledAt) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            throw new ValidacionPayloadException("payload requerido", List.of("payload"));
        }
        String audienceMode = normalizeAudienceMode(request.getAudienceMode());
        if (!"all".equals(audienceMode) && !"selected".equals(audienceMode)) {
            errors.add("audienceMode debe ser all o selected");
        }
        if ("selected".equals(audienceMode)) {
            int userCount = request.getUserIds() == null ? 0 : (int) request.getUserIds().stream().filter(Objects::nonNull).distinct().count();
            if (userCount <= 0) {
                errors.add("userIds requeridos para selected");
            }
            if (userCount > MAX_SCHEDULED_RECIPIENTS) {
                errors.add("userIds excede maximo permitido");
            }
        }
        String plainMessage = request.getMessage();
        if (StringUtils.hasText(plainMessage) && plainMessage.trim().length() > MAX_SCHEDULED_MESSAGE_LENGTH) {
            errors.add("message excede longitud maxima");
        }
        if (scheduledAt == null || !scheduledAt.isAfter(Instant.now())) {
            errors.add("scheduledAt debe ser futuro UTC");
        }
        boolean hasEncryptedPayloads = hasEncryptedAdminPayloads(request.getEncryptedPayloads());
        boolean hasLegacyPlaintext = StringUtils.hasText(resolveLegacyAdminDirectContent(request));
        if (!hasEncryptedPayloads && !hasLegacyPlaintext) {
            errors.add("encryptedPayloads");
        } else if (hasEncryptedPayloads && request.getEncryptedPayloads().size() > MAX_SCHEDULED_RECIPIENTS) {
            errors.add("encryptedPayloads excede maximo permitido");
        }
        if (!errors.isEmpty()) {
            throw new ValidacionPayloadException("Payload invalido para programar mensaje administrativo", errors);
        }
    }

    private void validateScheduledBulkEmailRequest(BulkEmailRequestDTO request, List<MultipartFile> attachments) {
        if (request == null) {
            throw new ValidacionPayloadException("payload requerido", List.of("payload"));
        }
        List<String> errors = new ArrayList<>();
        String audienceMode = normalizeAudienceMode(request.getAudienceMode());
        if (!"all".equals(audienceMode) && !"selected".equals(audienceMode)) {
            errors.add("audienceMode debe ser all o selected");
        }
        if (!StringUtils.hasText(request.getSubject())) {
            errors.add("subject no puede estar vacio");
        } else if (request.getSubject().trim().length() > MAX_SCHEDULED_SUBJECT_LENGTH) {
            errors.add("subject excede longitud maxima");
        }
        if (!StringUtils.hasText(request.getBody())) {
            errors.add("body no puede estar vacio");
        } else if (request.getBody().trim().length() > MAX_SCHEDULED_BODY_LENGTH) {
            errors.add("body excede longitud maxima");
        }
        if (request.getScheduledAt() == null && !StringUtils.hasText(request.getScheduledAtLocal())) {
            errors.add("scheduledAt/fechaProgramada");
        } else if (request.getScheduledAt() != null && !request.getScheduledAt().isAfter(Instant.now())) {
            errors.add("scheduledAt debe ser futuro UTC");
        }
        int actualAttachmentCount = attachments == null ? 0 : (int) attachments.stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .count();
        int declaredAttachmentCount = request.getAttachmentCount() == null ? 0 : request.getAttachmentCount();
        if (declaredAttachmentCount != actualAttachmentCount) {
            errors.add("attachmentCount no coincide con attachments recibidos");
        }
        if (actualAttachmentCount > MAX_SCHEDULED_ATTACHMENTS) {
            errors.add("attachments excede maximo permitido");
        }
        if ("selected".equals(audienceMode)) {
            boolean hasEmails = request.getRecipientEmails() != null && request.getRecipientEmails().stream().anyMatch(StringUtils::hasText);
            boolean hasUserIds = request.getUserIds() != null && request.getUserIds().stream().anyMatch(Objects::nonNull);
            if (!hasEmails && !hasUserIds) {
                errors.add("recipientEmails o userIds requeridos para selected");
            }
        }
        if (request.getUserIds() != null && request.getUserIds().stream().filter(Objects::nonNull).distinct().count() > MAX_SCHEDULED_RECIPIENTS) {
            errors.add("userIds excede maximo permitido");
        }
        if (request.getRecipientEmails() != null && request.getRecipientEmails().stream().filter(StringUtils::hasText).count() > MAX_SCHEDULED_RECIPIENTS) {
            errors.add("recipientEmails excede maximo permitido");
        }
        if (request.getRecipientEmails() != null) {
            for (String email : request.getRecipientEmails()) {
                if (!StringUtils.hasText(email)) {
                    continue;
                }
                if (!isValidEmail(email)) {
                    errors.add("recipientEmails contiene email invalido");
                    break;
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidacionPayloadException("Bulk email payload invalido", errors);
        }
    }

    private List<ScheduledEmailRecipient> resolveScheduledEmailRecipients(BulkEmailRequestDTO request) {
        String audienceMode = normalizeAudienceMode(request.getAudienceMode());
        if ("all".equals(audienceMode)) {
            Long adminId = securityUtils.getAuthenticatedUserId();
            return usuarioRepository.findByActivoTrueAndIdNot(adminId).stream()
                    .filter(Objects::nonNull)
                    .filter(UsuarioEntity::isActivo)
                    .filter(user -> !isAdminUser(user))
                    .filter(user -> StringUtils.hasText(user.getEmail()))
                    .map(user -> new ScheduledEmailRecipient(user.getId(), user.getEmail().trim()))
                    .toList();
        }

        List<String> recipientEmails = request.getRecipientEmails() == null ? List.of() : request.getRecipientEmails().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(this::isValidEmail)
                .toList();
        List<Long> userIds = request.getUserIds() == null ? List.of() : request.getUserIds();

        if (!recipientEmails.isEmpty()) {
            List<ScheduledEmailRecipient> recipients = new ArrayList<>();
            for (int i = 0; i < recipientEmails.size(); i++) {
                Long userId = i < userIds.size() ? userIds.get(i) : null;
                recipients.add(new ScheduledEmailRecipient(userId, recipientEmails.get(i)));
            }
            return recipients;
        }

        return resolveAdminAudienceUsers("selected", userIds).stream()
                .filter(user -> StringUtils.hasText(user.getEmail()))
                .map(user -> new ScheduledEmailRecipient(user.getId(), user.getEmail().trim()))
                .toList();
    }

    private String persistScheduledAttachments(String batchId, List<MultipartFile> attachments) {
        List<StoredEmailAttachment> storedAttachments = new ArrayList<>();
        if (attachments != null) {
            for (MultipartFile attachment : attachments) {
                if (attachment == null || attachment.isEmpty()) {
                    continue;
                }
                if (attachment.getSize() > maxUploadFileBytes) {
                    throw new IllegalArgumentException("adjunto excede tamano maximo permitido");
                }
                try {
                    Path baseDir = Paths.get(uploadsRoot).toAbsolutePath().normalize()
                            .resolve(SCHEDULED_ATTACHMENTS_DIR)
                            .resolve(batchId);
                    Files.createDirectories(baseDir);
                    String safeName = sanitizeFileName(attachment.getOriginalFilename());
                    String safeMimeType = sanitizeAttachmentMimeType(attachment.getContentType());
                    Path target = baseDir.resolve(safeName).normalize();
                    if (!target.startsWith(baseDir)) {
                        throw new IllegalArgumentException("nombre de adjunto invalido");
                    }
                    Files.write(target, attachment.getBytes());
                    storedAttachments.add(new StoredEmailAttachment(
                            safeName,
                            safeMimeType,
                            batchId + "/" + safeName,
                            attachment.getSize()));
                } catch (Exception ex) {
                    throw new IllegalStateException("No se pudieron persistir adjuntos programados", ex);
                }
            }
        }
        try {
            return storedAttachments.isEmpty() ? null : OBJECT_MAPPER.writeValueAsString(storedAttachments);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar metadata de adjuntos programados", ex);
        }
    }

    private String sanitizeFileName(String originalName) {
        String candidate = StringUtils.hasText(originalName) ? originalName.trim() : "attachment";
        return candidate.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @Override
    public Page<MensajeProgramadoDTO> listarMensajesProgramados(EstadoMensajeProgramado status, Pageable pageable) {
        validateAdminOnly();
        List<MensajeProgramadoDTO> items = mensajeProgramadoRepository.findAdminScheduledRows().stream()
                .filter(this::isAdminScheduledRow)
                .collect(Collectors.groupingBy(this::resolveBatchKey, java.util.LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::toAdminScheduledDto)
                .filter(Objects::nonNull)
                .filter(dto -> status == null || status.name().equalsIgnoreCase(dto.getStatus()))
                .sorted((a, b) -> {
                    Instant left = a.getScheduledAt() == null ? Instant.EPOCH : a.getScheduledAt();
                    Instant right = b.getScheduledAt() == null ? Instant.EPOCH : b.getScheduledAt();
                    int byScheduled = right.compareTo(left);
                    if (byScheduled != 0) {
                        return byScheduled;
                    }
                    Long leftId = a.getId() == null ? 0L : a.getId();
                    Long rightId = b.getId() == null ? 0L : b.getId();
                    return rightId.compareTo(leftId);
                })
                .toList();
        int start = (int) Math.min(pageable.getOffset(), items.size());
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }

    @Override
    @Transactional
    public MensajeProgramadoDTO editarMensajeDirectoAdminProgramado(Long id, AdminDirectMessageScheduledRequestDTO request) {
        validateAdminOnly();
        if (request == null) {
            throw new ValidacionPayloadException("payload requerido", List.of("payload"));
        }

        MensajeProgramadoEntity root = mensajeProgramadoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje programado no encontrado: " + id));
        if (!isAdminScheduledRow(root) || isScheduledBulkEmail(root)) {
            throw new RecursoNoEncontradoException("Mensaje programado no encontrado: " + id);
        }

        List<MensajeProgramadoEntity> rows = mensajeProgramadoRepository.findAllByBatchOrSelfForUpdate(root.getScheduledBatchId(), root.getId())
                .stream()
                .filter(this::isAdminScheduledRow)
                .filter(row -> !isScheduledBulkEmail(row))
                .toList();
        if (rows.isEmpty()) {
            throw new RecursoNoEncontradoException("Mensaje programado no encontrado: " + id);
        }
        if (rows.stream().anyMatch(row -> row.getStatus() != EstadoMensajeProgramado.PENDING)) {
            throw new ValidacionPayloadException("Solo se pueden editar mensajes programados pendientes", List.of("status"));
        }

        Instant scheduledAt = rows.stream()
                .map(MensajeProgramadoEntity::getScheduledAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(root.getScheduledAt());
        if (scheduledAt == null || !scheduledAt.isAfter(Instant.now())) {
            throw new ValidacionPayloadException("El mensaje programado ya no es editable", List.of("scheduledAt"));
        }

        AdminScheduledPayload currentPayload = parseAdminPayload(rows.get(0).getAdminPayload());
        List<UsuarioEntity> destinatarios = resolveScheduledDirectRecipients(rows, currentPayload);
        List<String> errors = new ArrayList<>();
        Map<Long, AdminDirectMessagePayloadDTO> payloadsByChatId = validateAndMapScheduledAdminPayloadsForRows(
                request.getEncryptedPayloads(),
                rows,
                scheduledAt,
                errors);
        if (!errors.isEmpty()) {
            throw new ValidacionPayloadException("Payload invalido para editar mensaje administrativo programado", errors);
        }

        long expiresAfterReadSeconds = request.getExpiresAfterReadSeconds() == null
                ? rows.get(0).getExpiresAfterReadSeconds() == null ? normalizeAdminDirectExpiry(null) : rows.get(0).getExpiresAfterReadSeconds()
                : normalizeAdminDirectExpiry(request.getExpiresAfterReadSeconds());
        List<ScheduledRecipientUserDTO> recipientUsers = currentPayload != null && currentPayload.recipientUsers() != null && !currentPayload.recipientUsers().isEmpty()
                ? currentPayload.recipientUsers()
                : buildRecipientUsers(destinatarios);
        String audienceMode = currentPayload == null ? null : currentPayload.audienceMode();
        boolean encryptedFlow = hasEncryptedAdminPayloads(request.getEncryptedPayloads());
        String plainMessage = !encryptedFlow && request.getMessage() != null
                ? sanitizePlainText(request.getMessage(), MAX_SCHEDULED_MESSAGE_LENGTH)
                : currentPayload == null ? null : currentPayload.message();
        String adminPayload = serializeAdminPayload(new AdminScheduledPayload(
                "DIRECT_MESSAGE",
                audienceMode,
                plainMessage,
                null,
                null,
                collectUserIds(recipientUsers),
                collectRecipientEmails(recipientUsers),
                recipientUsers,
                List.of(),
                expiresAfterReadSeconds));

        for (MensajeProgramadoEntity row : rows) {
            Long chatId = row.getChat() == null ? null : row.getChat().getId();
            Long expectedUserId = resolveScheduledDirectRecipientUserId(row);
            AdminDirectMessagePayloadDTO payload = resolveScheduledEditPayload(payloadsByChatId, request.getEncryptedPayloads(), chatId, expectedUserId);
            if (payload == null) {
                throw new ValidacionPayloadException("Payload invalido para editar mensaje administrativo programado",
                        List.of(expectedUserId == null
                                ? "encryptedPayloads faltante para chatId=" + chatId
                                : "encryptedPayloads faltante para userId=" + expectedUserId));
            }
            row.setMessageContent(payload.getContenido());
            row.setAdminPayload(adminPayload);
            row.setExpiresAfterReadSeconds(expiresAfterReadSeconds);
            row.setLastError(null);
        }

        List<MensajeProgramadoEntity> saved = mensajeProgramadoRepository.saveAll(rows);
        LOGGER.info("[SCHEDULED_ADMIN_DIRECT_EDIT] adminId={} scheduledId={} batchId={} recipients={} scheduledAtUTC={}",
                securityUtils.getAuthenticatedUserId(),
                id,
                root.getScheduledBatchId(),
                saved.size(),
                scheduledAt);
        return toAdminScheduledDto(saved);
    }

    @Override
    @Transactional
    public MensajeProgramadoDTO cancelarMensajeProgramado(Long id) {
        validateAdminOnly();
        MensajeProgramadoEntity row = mensajeProgramadoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje programado no encontrado: " + id));
        if (!isAdminScheduledRow(row)) {
            throw new RecursoNoEncontradoException("Mensaje programado no encontrado: " + id);
        }
        List<MensajeProgramadoEntity> rows = mensajeProgramadoRepository.findAllByBatchOrSelfForUpdate(row.getScheduledBatchId(), row.getId());
        UsuarioEntity admin = usuarioRepository.findById(securityUtils.getAuthenticatedUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));
        Instant now = Instant.now();
        boolean changed = false;
        for (MensajeProgramadoEntity item : rows) {
            if (item.getStatus() == EstadoMensajeProgramado.PENDING || item.getStatus() == EstadoMensajeProgramado.PROCESSING) {
                item.setStatus(EstadoMensajeProgramado.CANCELED);
                item.setLockToken(null);
                item.setLockUntil(null);
                item.setLastError(null);
                item.setCanceledBy(admin);
                item.setCanceledAt(now);
                changed = true;
            }
        }
        if (changed) {
            mensajeProgramadoRepository.saveAll(rows);
        }
        return toAdminScheduledDto(rows);
    }

    private boolean isAdminScheduledRow(MensajeProgramadoEntity row) {
        return row != null && (row.isAdminMessage()
                || Constantes.SCHEDULED_DELIVERY_TYPE_ADMIN_BULK_EMAIL.equalsIgnoreCase(row.getDeliveryType()));
    }

    private String resolveBatchKey(MensajeProgramadoEntity row) {
        if (row == null) {
            return "null";
        }
        if (StringUtils.hasText(row.getScheduledBatchId())) {
            return row.getScheduledBatchId().trim();
        }
        return "row:" + row.getId();
    }

    private List<UsuarioEntity> resolveScheduledDirectRecipients(List<MensajeProgramadoEntity> rows, AdminScheduledPayload payload) {
        List<Long> userIds = payload == null || payload.userIds() == null || payload.userIds().isEmpty()
                ? rows.stream()
                .map(this::resolveScheduledDirectRecipientUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList()
                : payload.userIds().stream().filter(Objects::nonNull).distinct().toList();
        return resolveAdminAudienceUsers("selected", userIds);
    }

    private Map<Long, AdminDirectMessagePayloadDTO> validateAndMapScheduledAdminPayloadsForRows(List<AdminDirectMessagePayloadDTO> encryptedPayloads,
                                                                                                List<MensajeProgramadoEntity> rows,
                                                                                                Instant scheduledAt,
                                                                                                List<String> errors) {
        if (encryptedPayloads == null || encryptedPayloads.isEmpty()) {
            errors.add("encryptedPayloads");
            return Map.of();
        }
        Long authUserId = securityUtils.getAuthenticatedUserId();
        Map<Long, MensajeProgramadoEntity> rowsByChatId = rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> row.getChat() != null && row.getChat().getId() != null)
                .collect(Collectors.toMap(
                        row -> row.getChat().getId(),
                        row -> row,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new));
        Map<Long, Long> expectedChatIdByUserId = rows.stream()
                .filter(Objects::nonNull)
                .map(row -> Map.entry(resolveScheduledDirectRecipientUserId(row), row.getChat() == null ? null : row.getChat().getId()))
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, java.util.LinkedHashMap::new));
        Map<Long, AdminDirectMessagePayloadDTO> payloadsByChatId = new java.util.LinkedHashMap<>();
        for (AdminDirectMessagePayloadDTO payload : encryptedPayloads) {
            if (payload == null) {
                errors.add("encryptedPayloads contiene item null");
                continue;
            }
            if (payload.getUserId() == null) {
                errors.add("encryptedPayloads.userId es obligatorio");
                continue;
            }
            Long resolvedChatId = payload.getChatId() != null ? payload.getChatId() : expectedChatIdByUserId.get(payload.getUserId());
            if (resolvedChatId == null) {
                errors.add("encryptedPayloads.userId no coincide con batch para userId=" + payload.getUserId());
                continue;
            }
            MensajeProgramadoEntity row = rowsByChatId.get(resolvedChatId);
            if (row == null) {
                errors.add("encryptedPayloads contiene chatId no incluido en el batch: " + resolvedChatId);
                continue;
            }
            if (!StringUtils.hasText(payload.getContenido())) {
                errors.add("encryptedPayloads.contenido es obligatorio para userId=" + payload.getUserId());
                continue;
            }
            if (payloadsByChatId.putIfAbsent(resolvedChatId, payload) != null) {
                errors.add("encryptedPayloads duplicado para " + (payload.getChatId() != null ? "chatId=" + resolvedChatId : "userId=" + payload.getUserId()));
                continue;
            }
            Long expectedChatId = row.getChat() == null ? null : row.getChat().getId();
            if (payload.getChatId() != null && !Objects.equals(payload.getChatId(), expectedChatId)) {
                errors.add("encryptedPayloads.chatId no coincide para userId=" + payload.getUserId());
                continue;
            }
            Long expectedUserId = resolveScheduledDirectRecipientUserId(row);
            if (expectedUserId != null && !Objects.equals(payload.getUserId(), expectedUserId)) {
                errors.add("encryptedPayloads.userId no coincide para chatId=" + payload.getChatId());
                continue;
            }
            ValidatedE2EPayload validated = validarPayloadE2EProgramadoOrThrow(
                    payload.getContenido(),
                    authUserId,
                    expectedChatId == null ? List.of() : List.of(expectedChatId),
                    scheduledAt);
            if (validated == null) {
                errors.add("encryptedPayloads.contenido invalido para userId=" + payload.getUserId());
            }
        }
        for (Long chatId : rowsByChatId.keySet()) {
            if (!payloadsByChatId.containsKey(chatId)) {
                errors.add("encryptedPayloads faltante para chatId=" + chatId);
            }
        }
        return payloadsByChatId;
    }

    private Long resolveScheduledDirectRecipientUserId(MensajeProgramadoEntity row) {
        UsuarioEntity recipient = resolveScheduledDirectRecipientUser(row);
        return recipient == null ? null : recipient.getId();
    }

    private boolean hasEncryptedAdminPayloads(List<AdminDirectMessagePayloadDTO> encryptedPayloads) {
        return encryptedPayloads != null && !encryptedPayloads.isEmpty();
    }

    private String resolveLegacyAdminDirectContent(AdminDirectMessageScheduledRequestDTO request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.hasText(request.getContenido())) {
            return request.getContenido();
        }
        return request.getMessage();
    }

    private AdminDirectMessagePayloadDTO resolveScheduledEditPayload(Map<Long, AdminDirectMessagePayloadDTO> payloadsByChatId,
                                                                    List<AdminDirectMessagePayloadDTO> rawPayloads,
                                                                    Long chatId,
                                                                    Long userId) {
        if (chatId != null && payloadsByChatId != null) {
            AdminDirectMessagePayloadDTO byChat = payloadsByChatId.get(chatId);
            if (byChat != null) {
                return byChat;
            }
        }
        if (userId == null || rawPayloads == null) {
            return null;
        }
        return rawPayloads.stream()
                .filter(Objects::nonNull)
                .filter(payload -> Objects.equals(userId, payload.getUserId()))
                .findFirst()
                .orElse(null);
    }

    private UsuarioEntity resolveScheduledDirectRecipientUser(MensajeProgramadoEntity row) {
        if (row == null || !(row.getChat() instanceof ChatIndividualEntity chat)) {
            return null;
        }
        Long adminId = row.getCreatedBy() == null ? null : row.getCreatedBy().getId();
        if (chat.getUsuario1() != null && !Objects.equals(chat.getUsuario1().getId(), adminId)) {
            return chat.getUsuario1();
        }
        return chat.getUsuario2();
    }

    private MensajeProgramadoDTO toAdminScheduledDto(List<MensajeProgramadoEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        List<MensajeProgramadoEntity> sorted = rows.stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    int byId = Long.compare(a.getId() == null ? 0L : a.getId(), b.getId() == null ? 0L : b.getId());
                    if (byId != 0) {
                        return byId;
                    }
                    Instant left = a.getScheduledAt() == null ? Instant.EPOCH : a.getScheduledAt();
                    Instant right = b.getScheduledAt() == null ? Instant.EPOCH : b.getScheduledAt();
                    return right.compareTo(left);
                })
                .toList();
        MensajeProgramadoEntity first = sorted.get(0);
        AdminScheduledPayload payload = parseAdminPayload(first.getAdminPayload());
        MensajeProgramadoDTO dto = mensajeProgramadoMapper.toDto(first);
        List<ScheduledRecipientUserDTO> resolvedRecipientUsers = resolveRecipientUsers(sorted, payload);
        dto.setId(first.getId());
        dto.setChatId(first.getChat() == null ? null : first.getChat().getId());
        dto.setCreatedBy(first.getCreatedBy() == null ? null : first.getCreatedBy().getId());
        dto.setCanceledBy(first.getCanceledBy() == null ? null : first.getCanceledBy().getId());
        dto.setCanceledAt(resolveCanceledAt(sorted));
        dto.setDeliveryType(resolveFrontDeliveryType(first));
        dto.setType(payload == null ? resolveFrontType(first) : payload.type());
        dto.setAudienceMode(payload == null ? null : payload.audienceMode());
        dto.setMessage(payload == null ? null : payload.message());
        dto.setContenido(isScheduledBulkEmail(first) ? null : first.getMessageContent());
        dto.setSubject(first.getEmailSubject());
        dto.setBody(isScheduledBulkEmail(first) ? first.getMessageContent() : null);
        dto.setUserIds(resolveUserIds(payload, resolvedRecipientUsers));
        dto.setRecipientEmails(resolveRecipientEmails(sorted, payload));
        dto.setRecipientUsers(resolvedRecipientUsers);
        dto.setRecipientCount(dto.getRecipientUsers() == null ? 0 : dto.getRecipientUsers().size());
        dto.setRecipientLabel(buildRecipientLabel(payload, dto.getRecipientCount()));
        dto.setRecipientsSummary(dto.getRecipientLabel());
        dto.setAttachmentNames(resolveAttachmentNames(first, payload));
        dto.setAttachmentsMeta(resolveAttachmentsMeta(first, payload));
        dto.setAttachmentCount(dto.getAttachmentsMeta() == null ? 0 : dto.getAttachmentsMeta().size());
        dto.setStatus(resolveAggregateStatus(sorted).name());
        dto.setAttempts(sorted.stream().map(MensajeProgramadoEntity::getAttempts).filter(Objects::nonNull).max(Integer::compareTo).orElse(0));
        dto.setLastError(resolveAggregateLastError(sorted));
        dto.setCreatedAt(sorted.stream().map(MensajeProgramadoEntity::getCreatedAt).filter(Objects::nonNull).min(Instant::compareTo).orElse(first.getCreatedAt()));
        dto.setUpdatedAt(sorted.stream().map(MensajeProgramadoEntity::getUpdatedAt).filter(Objects::nonNull).max(Instant::compareTo).orElse(first.getUpdatedAt()));
        dto.setSentAt(sorted.stream().map(MensajeProgramadoEntity::getSentAt).filter(Objects::nonNull).max(Instant::compareTo).orElse(null));
        dto.setMessageContent(first.getMessageContent());
        return dto;
    }

    private EstadoMensajeProgramado resolveAggregateStatus(List<MensajeProgramadoEntity> rows) {
        boolean hasProcessing = rows.stream().anyMatch(row -> row.getStatus() == EstadoMensajeProgramado.PROCESSING);
        if (hasProcessing) {
            return EstadoMensajeProgramado.PROCESSING;
        }
        boolean hasPending = rows.stream().anyMatch(row -> row.getStatus() == EstadoMensajeProgramado.PENDING);
        if (hasPending) {
            return EstadoMensajeProgramado.PENDING;
        }
        boolean hasFailed = rows.stream().anyMatch(row -> row.getStatus() == EstadoMensajeProgramado.FAILED);
        if (hasFailed) {
            return EstadoMensajeProgramado.FAILED;
        }
        boolean hasSent = rows.stream().anyMatch(row -> row.getStatus() == EstadoMensajeProgramado.SENT);
        if (hasSent) {
            return EstadoMensajeProgramado.SENT;
        }
        return EstadoMensajeProgramado.CANCELED;
    }

    private String resolveAggregateLastError(List<MensajeProgramadoEntity> rows) {
        return rows.stream()
                .map(MensajeProgramadoEntity::getLastError)
                .filter(StringUtils::hasText)
                .map(this::truncarError)
                .findFirst()
                .orElse(null);
    }

    private Instant resolveCanceledAt(List<MensajeProgramadoEntity> rows) {
        return rows.stream()
                .map(MensajeProgramadoEntity::getCanceledAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private String resolveFrontDeliveryType(MensajeProgramadoEntity row) {
        return isScheduledBulkEmail(row) ? "email" : "message";
    }

    private String resolveFrontType(MensajeProgramadoEntity row) {
        return isScheduledBulkEmail(row) ? "BULK_EMAIL" : "DIRECT_MESSAGE";
    }

    private String buildRecipientLabel(AdminScheduledPayload payload, int recipientCount) {
        String audienceMode = payload == null ? null : payload.audienceMode();
        if ("all".equalsIgnoreCase(audienceMode)) {
            return recipientCount + " usuarios";
        }
        return recipientCount + (recipientCount == 1 ? " usuario seleccionado" : " usuarios seleccionados");
    }

    private List<String> resolveRecipientEmails(List<MensajeProgramadoEntity> rows, AdminScheduledPayload payload) {
        if (payload != null && payload.recipientEmails() != null && !payload.recipientEmails().isEmpty()) {
            return payload.recipientEmails();
        }
        return rows.stream()
                .map(MensajeProgramadoEntity::getRecipientEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<Long> resolveUserIds(AdminScheduledPayload payload, List<ScheduledRecipientUserDTO> recipientUsers) {
        if (payload != null && payload.userIds() != null && !payload.userIds().isEmpty()) {
            return payload.userIds();
        }
        return collectUserIds(recipientUsers);
    }

    private List<ScheduledRecipientUserDTO> resolveRecipientUsers(List<MensajeProgramadoEntity> rows, AdminScheduledPayload payload) {
        if (!isScheduledBulkEmail(rows.get(0))) {
            Map<Long, ScheduledRecipientUserDTO> payloadUsersById = payload != null && payload.recipientUsers() != null
                    ? payload.recipientUsers().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item.getUserId() != null)
                    .collect(Collectors.toMap(
                            ScheduledRecipientUserDTO::getUserId,
                            item -> item,
                            (left, right) -> left))
                    : java.util.Collections.emptyMap();
            return rows.stream()
                    .map(row -> {
                        Long recipientUserId = resolveScheduledDirectRecipientUserId(row);
                        UsuarioEntity recipientUser = resolveScheduledDirectRecipientUser(row);
                        ScheduledRecipientUserDTO payloadUser = recipientUserId == null ? null : payloadUsersById.get(recipientUserId);
                        Long chatId = row.getChat() == null ? null : row.getChat().getId();
                        if (recipientUser != null) {
                            String email = payloadUser != null && StringUtils.hasText(payloadUser.getEmail()) ? payloadUser.getEmail() : recipientUser.getEmail();
                            String fullName = payloadUser != null && StringUtils.hasText(payloadUser.getFullName()) ? payloadUser.getFullName() : buildFullName(recipientUser);
                            return toRecipientUser(recipientUser.getId(), chatId, email, fullName);
                        }
                        return toRecipientUser(
                                recipientUserId,
                                chatId,
                                payloadUser == null ? null : payloadUser.getEmail(),
                                payloadUser == null ? null : payloadUser.getFullName());
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
        if (payload != null && payload.recipientUsers() != null && !payload.recipientUsers().isEmpty()) {
            return payload.recipientUsers();
        }
        if (isScheduledBulkEmail(rows.get(0))) {
            return rows.stream()
                    .map(row -> toRecipientUser(null, null, row.getRecipientEmail(), null))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return List.of();
    }

    private List<String> resolveAttachmentNames(MensajeProgramadoEntity first, AdminScheduledPayload payload) {
        List<ScheduledAttachmentMetaDTO> meta = resolveAttachmentsMeta(first, payload);
        return meta.stream().map(ScheduledAttachmentMetaDTO::getFileName).filter(StringUtils::hasText).toList();
    }

    private List<ScheduledAttachmentMetaDTO> resolveAttachmentsMeta(MensajeProgramadoEntity first, AdminScheduledPayload payload) {
        if (payload != null && payload.attachmentsMeta() != null && !payload.attachmentsMeta().isEmpty()) {
            return payload.attachmentsMeta();
        }
        return readAttachmentMeta(first.getAttachmentPayload());
    }

    @Override
    @Transactional
    public List<Long> reclamarMensajesVencidos(Instant ahora, String lockToken, int limite, int lockSeconds) {
        int batchSize = Math.max(1, limite);
        Instant now = ahora == null ? Instant.now() : ahora;
        if (lockToken == null || lockToken.isBlank()) {
            return List.of();
        }

        Instant lockUntil = now.plusSeconds(Math.max(10, lockSeconds));
        int updated = jdbcTemplate.update(
                "UPDATE chat_scheduled_message " +
                        "SET status = 'PROCESSING', lock_token = ?, lock_until = ?, updated_at = ? " +
                        "WHERE (status = 'PENDING' OR (status = 'PROCESSING' AND lock_until IS NOT NULL AND lock_until < ?)) " +
                        "AND scheduled_at <= ? " +
                        "AND (lock_until IS NULL OR lock_until < ?) " +
                        "ORDER BY scheduled_at ASC, id ASC " +
                        "LIMIT ?",
                lockToken,
                lockUntil,
                now,
                now,
                now,
                now,
                batchSize);

        if (updated <= 0) {
            LOGGER.info("[SCHEDULED_CLAIM] nowUTC={} updated={} token={} batchSize={} lockUntilUTC={}",
                    now,
                    updated,
                    lockToken,
                    batchSize,
                    lockUntil);
            return List.of();
        }
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM chat_scheduled_message " +
                        "WHERE status = 'PROCESSING' AND lock_token = ? " +
                        "ORDER BY scheduled_at ASC, id ASC",
                Long.class,
                lockToken);
        LOGGER.info("[SCHEDULED_CLAIM] nowUTC={} updated={} selectedIds={} token={} batchSize={} lockUntilUTC={}",
                now,
                updated,
                ids,
                lockToken,
                batchSize,
                lockUntil);
        return ids;
    }

    @Override
    @Transactional
    public void procesarMensajeProgramado(Long id, String lockToken) {
        MensajeProgramadoEntity row = mensajeProgramadoRepository.findByIdForUpdate(id).orElse(null);
        if (row == null) {
            LOGGER.info("[SCHEDULED_PROCESS_SKIP] scheduledId={} reason=not_found token={}", id, lockToken);
            return;
        }
        EstadoMensajeProgramado estadoAnterior = row.getStatus();
        if (row.getStatus() == EstadoMensajeProgramado.SENT || row.getStatus() == EstadoMensajeProgramado.CANCELED) {
            LOGGER.info("[SCHEDULED_PROCESS_SKIP] scheduledId={} reason=final_state status={} token={}", row.getId(), row.getStatus(), lockToken);
            return;
        }
        if (row.getStatus() != EstadoMensajeProgramado.PROCESSING || !Objects.equals(lockToken, row.getLockToken())) {
            LOGGER.info("[SCHEDULED_PROCESS_SKIP] scheduledId={} reason=lock_or_status_mismatch status={} rowToken={} requestToken={}",
                    row.getId(),
                    row.getStatus(),
                    row.getLockToken(),
                    lockToken);
            return;
        }

        int intentoActual = (row.getAttempts() == null ? 0 : row.getAttempts()) + 1;
        row.setAttempts(intentoActual);
        LOGGER.info("[SCHEDULED_PROCESS_START] scheduledId={} deliveryType={} chatId={} recipientEmail={} scheduledAtUTC={} nowUTC={} attempts={} statusBefore={}",
                row.getId(),
                row.getDeliveryType(),
                row.getChat() == null ? null : row.getChat().getId(),
                row.getRecipientEmail(),
                row.getScheduledAt(),
                Instant.now(),
                intentoActual,
                estadoAnterior);

        Instant nowUtc = Instant.now();
        if (row.getScheduledAt() != null && row.getScheduledAt().isAfter(nowUtc)) {
            row.setStatus(EstadoMensajeProgramado.PENDING);
            row.setLockToken(null);
            row.setLockUntil(null);
            row.setAttempts(Math.max(0, intentoActual - 1));
            mensajeProgramadoRepository.save(row);
            LOGGER.warn("[SCHEDULED_EARLY_CLAIM_GUARD] scheduledId={} deliveryType={} scheduledAtUTC={} nowUTC={} secondsEarly={} action=RETURN_TO_PENDING",
                    row.getId(),
                    row.getDeliveryType(),
                    row.getScheduledAt(),
                    nowUtc,
                    row.getScheduledAt().getEpochSecond() - nowUtc.getEpochSecond());
            return;
        }

        try {
            if (isScheduledBulkEmail(row)) {
                procesarBulkEmailProgramado(row, estadoAnterior, intentoActual);
                return;
            }
            PersistenciaProgramadaResultado resultado = prepararMensajeParaEnvio(row);
            MensajeDTO enviado = resultado.mensaje();
            row.setPersistedMessageId(enviado == null ? null : enviado.getId());
            row.setLastError(null);
            mensajeProgramadoRepository.save(row);
            programarEmisionWsAfterCommit(row, enviado, resultado.e2eType(), resultado.mode(), estadoAnterior, intentoActual);
        } catch (Exception ex) {
            boolean noRecuperable = esErrorNoRecuperable(ex);
            boolean agotado = intentoActual >= Math.max(1, maxAttempts);
            EstadoMensajeProgramado nuevoEstado = (noRecuperable || agotado)
                    ? EstadoMensajeProgramado.FAILED
                    : EstadoMensajeProgramado.PENDING;

            row.setStatus(nuevoEstado);
            row.setLastError(truncarError(ex.getMessage()));
            row.setLockToken(null);
            row.setLockUntil(null);
            row.setWsEmitted(false);
            row.setWsEmittedAt(null);
            row.setWsDestinations(null);
            row.setWsEmitError(truncarError(ex.getMessage()));
            mensajeProgramadoRepository.save(row);
            LOGGER.warn("[SCHEDULED_MESSAGE_ITEM] id={} chatId={} {}->{} attempts={} mode={} tipo={} error={}",
                    row.getId(),
                    row.getChat() == null ? null : row.getChat().getId(),
                    estadoAnterior,
                    row.getStatus(),
                    intentoActual,
                    resolverModoProcesamiento(row.getMessageContent()),
                    resolverTipoMensajeProgramado(row.getMessageContent()),
                    truncarError(ex.getMessage()));
            if (noRecuperable) {
                LOGGER.error("[SCHEDULED_MESSAGE_ITEM_NON_RECOVERABLE] id={} chatId={} errorType={} message={}",
                        row.getId(),
                        row.getChat() == null ? null : row.getChat().getId(),
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    private ChatEntity validarPermisoSobreChat(Long chatId, Long userId) {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId es obligatorio");
        }
        if (userId == null) {
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));

        if (chat instanceof ChatIndividualEntity) {
            ChatIndividualEntity ci = chatIndividualRepository.findById(chatId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));
            boolean miembro = (ci.getUsuario1() != null && Objects.equals(ci.getUsuario1().getId(), userId))
                    || (ci.getUsuario2() != null && Objects.equals(ci.getUsuario2().getId(), userId));
            if (!miembro) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            return ci;
        }

        if (chat instanceof ChatGrupalEntity) {
            ChatGrupalEntity cg = chatGrupalRepository.findByIdWithUsuarios(chatId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
            if (!cg.isActivo()) {
                throw new AccessDeniedException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO);
            }
            boolean miembroActivo = cg.getUsuarios() != null && cg.getUsuarios().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(u -> Objects.equals(u.getId(), userId) && u.isActivo());
            if (!miembroActivo) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
            }
            return cg;
        }

        throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
    }

    private long normalizeAdminDirectExpiry(Long requestedSeconds) {
        if (requestedSeconds == null || requestedSeconds <= 0) {
            return DEFAULT_ADMIN_DIRECT_EXPIRES_AFTER_READ_SECONDS;
        }
        return requestedSeconds;
    }

    private String normalizeForSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
    }

    private boolean isScheduledBulkEmail(MensajeProgramadoEntity row) {
        return row != null && Constantes.SCHEDULED_DELIVERY_TYPE_ADMIN_BULK_EMAIL.equalsIgnoreCase(row.getDeliveryType());
    }

    private void procesarBulkEmailProgramado(MensajeProgramadoEntity row,
                                             EstadoMensajeProgramado estadoAnterior,
                                             int intentoActual) {
        try {
            LOGGER.info("[SCHEDULED_ADMIN_BULK_EMAIL_SEND_START] scheduledId={} recipientEmail={} scheduledAtUTC={} nowUTC={} attempts={}",
                    row.getId(),
                    row.getRecipientEmail(),
                    row.getScheduledAt(),
                    Instant.now(),
                    intentoActual);
            Map<String, String> variables = new HashMap<>();
            variables.put(Constantes.EMAIL_VAR_SUBJECT, sanitizeEmailVariable(row.getEmailSubject()));
            variables.put(Constantes.EMAIL_VAR_BODY, sanitizeEmailBody(row.getMessageContent()));
            emailService.sendHtmlEmailWithAttachmentsOrThrow(
                    row.getRecipientEmail(),
                    row.getEmailSubject(),
                    Constantes.EMAIL_TEMPLATE_ADMIN_BULK,
                    variables,
                    loadScheduledEmailAttachments(row.getAttachmentPayload()));

            row.setStatus(EstadoMensajeProgramado.SENT);
            row.setSentAt(Instant.now());
            row.setLastError(null);
            row.setLockToken(null);
            row.setLockUntil(null);
            row.setWsEmitted(false);
            row.setWsEmittedAt(null);
            row.setWsDestinations("EMAIL:" + row.getRecipientEmail());
            row.setWsEmitError(null);
            mensajeProgramadoRepository.save(row);
            LOGGER.info("[SCHEDULED_ADMIN_BULK_EMAIL_SENT] id={} recipientEmail={} {}->{} attempts={} sentAtUTC={} scheduledAtUTC={} nowUTC={}",
                    row.getId(),
                    row.getRecipientEmail(),
                    estadoAnterior,
                    row.getStatus(),
                    intentoActual,
                    row.getSentAt(),
                    row.getScheduledAt(),
                    Instant.now());
        } catch (Exception ex) {
            boolean noRecuperable = esErrorNoRecuperable(ex);
            boolean agotado = intentoActual >= Math.max(1, maxAttempts);
            EstadoMensajeProgramado nuevoEstado = (noRecuperable || agotado)
                    ? EstadoMensajeProgramado.FAILED
                    : EstadoMensajeProgramado.PENDING;
            row.setStatus(nuevoEstado);
            row.setLastError(truncarError(ex.getMessage()));
            row.setLockToken(null);
            row.setLockUntil(null);
            row.setWsDestinations("EMAIL:" + row.getRecipientEmail());
            row.setWsEmitError(truncarError(ex.getMessage()));
            mensajeProgramadoRepository.save(row);
            LOGGER.warn("[SCHEDULED_ADMIN_BULK_EMAIL_ITEM] id={} recipientEmail={} {}->{} attempts={} error={}",
                    row.getId(),
                    row.getRecipientEmail(),
                    estadoAnterior,
                    row.getStatus(),
                    intentoActual,
                    truncarError(ex.getMessage()));
            if (noRecuperable) {
                LOGGER.error("[SCHEDULED_ADMIN_BULK_EMAIL_NON_RECOVERABLE] id={} recipientEmail={} errorType={} message={}",
                        row.getId(),
                        row.getRecipientEmail(),
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    private PersistenciaProgramadaResultado prepararMensajeParaEnvio(MensajeProgramadoEntity row) {
        if (row.getPersistedMessageId() != null) {
            MensajeEntity persisted = mensajeRepository.findById(row.getPersistedMessageId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Mensaje ya persistido no encontrado: " + row.getPersistedMessageId()));
            MensajeDTO dto = MappingUtils.mensajeEntityADto(persisted);
            if (persisted.getEmisor() != null) {
                dto.setEmisorNombre(persisted.getEmisor().getNombre());
                dto.setEmisorApellido(persisted.getEmisor().getApellido());
                dto.setEmisorFoto(persisted.getEmisor().getFotoUrl());
            }
            return new PersistenciaProgramadaResultado(
                    dto,
                    resolverTipoE2E(dto.getContenido()),
                    resolverModoProcesamiento(row.getMessageContent()));
        }
        return persistirMensajeComoEnvioNormal(row);
    }

    private PersistenciaProgramadaResultado persistirMensajeComoEnvioNormal(MensajeProgramadoEntity row) {
        UsuarioEntity emisor = Optional.ofNullable(row.getCreatedBy())
                .map(UsuarioEntity::getId)
                .map(this::cargarUsuarioConClaveActual)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        ChatEntity chat = row.getChat();
        if (chat == null || chat.getId() == null) {
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + null);
        }

        Long chatId = chat.getId();
        ChatEntity chatRef = chatRepository.findById(chatId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chat.getId()));

        Optional<ChatIndividualEntity> chatIndividualOpt = chatIndividualRepository.findById(chatId);
        Optional<ChatGrupalEntity> chatGrupalOpt = chatIndividualOpt.isPresent()
                ? Optional.empty()
                : chatGrupalRepository.findByIdWithUsuariosForUpdate(chatId);

        if (chatIndividualOpt.isEmpty() && chatGrupalOpt.isEmpty()) {
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId);
        }

        MensajeEntity mensaje = new MensajeEntity();
        mensaje.setEmisor(emisor);
        mensaje.setChat(chatRef);
        mensaje.setTipo(MessageType.TEXT);
        CifradorE2EMensajeProgramadoService.ResultadoCifradoProgramado cifradoProgramado = null;
        ValidatedE2EPayload passthroughPayload = parsePayloadE2EProgramado(row.getMessageContent());
        String modoProcesamiento = passthroughPayload == null ? MODE_LEGACY_ENCRYPT : MODE_PASSTHROUGH_E2E;
        String payloadJsonFinal = null;
        String e2eTypeFinal = null;
        String rsaRuntimeFinal = "PASSTHROUGH";
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setActivo(true);
        mensaje.setLeido(false);
        mensaje.setReenviado(false);
        mensaje.setMensajeTemporal(row.isMessageTemporal());
        mensaje.setMensajeTemporalSegundos(row.isMessageTemporal() ? row.getExpiresAfterReadSeconds() : null);
        mensaje.setExpiraEn(null);
        mensaje.setMotivoEliminacion(null);
        mensaje.setPlaceholderTexto(null);
        mensaje.setAdminMessage(row.isAdminMessage());
        mensaje.setExpiresAfterReadSeconds(row.isAdminMessage() ? row.getExpiresAfterReadSeconds() : null);
        mensaje.setFirstReadAt(null);
        mensaje.setExpireAt(null);
        mensaje.setExpiredByPolicy(false);

        if (chatIndividualOpt.isPresent()) {
            ChatIndividualEntity ci = chatIndividualOpt.get();
            Long emisorId = emisor.getId();
            boolean emisorPertenece = (ci.getUsuario1() != null && Objects.equals(ci.getUsuario1().getId(), emisorId))
                    || (ci.getUsuario2() != null && Objects.equals(ci.getUsuario2().getId(), emisorId));
            if (!emisorPertenece) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            if (ci.getUsuario1() != null && Objects.equals(ci.getUsuario1().getId(), emisorId)) {
                Long receptorId = ci.getUsuario2() == null ? null : ci.getUsuario2().getId();
                mensaje.setReceptor(receptorId == null ? null : cargarUsuarioConClaveActual(receptorId));
            } else {
                Long receptorId = ci.getUsuario1() == null ? null : ci.getUsuario1().getId();
                mensaje.setReceptor(receptorId == null ? null : cargarUsuarioConClaveActual(receptorId));
            }
            if (mensaje.getReceptor() == null || mensaje.getReceptor().getId() == null) {
                throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_INDIVIDUAL_NO_ENCONTRADO);
            }
            if (emisor.getBloqueados().contains(mensaje.getReceptor())
                    || mensaje.getReceptor().getBloqueados().contains(emisor)) {
                throw new AccessDeniedException(Constantes.MSG_NO_PUEDE_ENVIAR_MENSAJES);
            }
            if (passthroughPayload != null) {
                if (!(TYPE_E2E.equals(passthroughPayload.type()) || TYPE_E2E_FILE.equals(passthroughPayload.type()))) {
                    throw new IllegalArgumentException("contenido E2E no compatible con chat individual");
                }
                payloadJsonFinal = row.getMessageContent();
                e2eTypeFinal = passthroughPayload.type();
                if (TYPE_E2E_FILE.equals(passthroughPayload.type())) {
                    mensaje.setTipo(MessageType.FILE);
                } else {
                    mensaje.setTipo(MessageType.TEXT);
                }
            } else {
                cifradoProgramado = cifradorE2EMensajeProgramadoService.cifrarTextoIndividual(
                        row.getMessageContent(),
                        emisor,
                        mensaje.getReceptor());
                payloadJsonFinal = E2EPayloadUtils.normalizeForStorage(cifradoProgramado.payloadJson());
                e2eTypeFinal = cifradoProgramado.e2eType();
                rsaRuntimeFinal = cifradoProgramado.rsaRuntimeAlgorithm();
                mensaje.setTipo(MessageType.TEXT);
            }
        } else {
            mensaje.setReceptor(null);
            ChatGrupalEntity chatGrupal = chatGrupalOpt
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
            if (!chatGrupal.isActivo()) {
                throw new AccessDeniedException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO);
            }
            if (chatGrupal.isClosed()) {
                throw new ChatCerradoException(Constantes.MSG_CHAT_GRUPAL_CERRADO, chatId);
            }
            boolean emisorEsMiembroActivo = chatGrupal.getUsuarios() != null && chatGrupal.getUsuarios().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(u -> Objects.equals(u.getId(), emisor.getId()) && u.isActivo());
            if (!emisorEsMiembroActivo) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
            }
            List<UsuarioEntity> receptoresActivos = chatGrupal.getUsuarios() == null
                    ? List.of()
                    : chatGrupal.getUsuarios().stream()
                    .filter(Objects::nonNull)
                    .filter(UsuarioEntity::isActivo)
                    .filter(u -> !Objects.equals(u.getId(), emisor.getId()))
                    .map(UsuarioEntity::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(this::cargarUsuarioConClaveActual)
                    .collect(Collectors.toList());
            if (passthroughPayload != null) {
                if (!(TYPE_E2E_GROUP.equals(passthroughPayload.type()) || TYPE_E2E_GROUP_FILE.equals(passthroughPayload.type()))) {
                    throw new IllegalArgumentException("contenido E2E no compatible con chat grupal");
                }
                payloadJsonFinal = row.getMessageContent();
                e2eTypeFinal = passthroughPayload.type();
                if (TYPE_E2E_GROUP_FILE.equals(passthroughPayload.type())) {
                    mensaje.setTipo(MessageType.FILE);
                } else {
                    mensaje.setTipo(MessageType.TEXT);
                }
            } else {
                cifradoProgramado = cifradorE2EMensajeProgramadoService.cifrarTextoGrupal(
                        row.getMessageContent(),
                        emisor,
                        receptoresActivos);
                payloadJsonFinal = E2EPayloadUtils.normalizeForStorage(cifradoProgramado.payloadJson());
                e2eTypeFinal = cifradoProgramado.e2eType();
                rsaRuntimeFinal = cifradoProgramado.rsaRuntimeAlgorithm();
                mensaje.setTipo(MessageType.TEXT);
            }
            mensaje.setChat(chatGrupal);
        }

        mensaje.setContenido(payloadJsonFinal);
        mensaje.setContenidoBusqueda(normalizeForSearch(payloadJsonFinal));

        MensajeEntity saved = mensajeRepository.save(mensaje);
        MensajeDTO out = MappingUtils.mensajeEntityADto(saved);
        if (emisor != null) {
            out.setEmisorNombre(emisor.getNombre());
            out.setEmisorApellido(emisor.getApellido());
            out.setEmisorFoto(emisor.getFotoUrl());
        }
        registrarDiagnosticoCifradoProgramado(
                row,
                saved,
                out,
                emisor,
                mensaje.getReceptor(),
                payloadJsonFinal,
                rsaRuntimeFinal);
        return new PersistenciaProgramadaResultado(out, e2eTypeFinal, modoProcesamiento);
    }

    private String resolverTipoE2E(String contenido) {
        String clasificacion = E2EDiagnosticUtils.analyze(contenido, Constantes.TIPO_TEXT).getClassification();
        if ("JSON_E2E_GROUP_FILE".equals(clasificacion)) {
            return TYPE_E2E_GROUP_FILE;
        }
        if ("JSON_E2E_FILE".equals(clasificacion)) {
            return TYPE_E2E_FILE;
        }
        if ("JSON_E2E_GROUP".equals(clasificacion)) {
            return TYPE_E2E_GROUP;
        }
        return TYPE_E2E;
    }

    private UsuarioEntity cargarUsuarioConClaveActual(Long userId) {
        return usuarioRepository.findFreshById(userId)
                .or(() -> usuarioRepository.findById(userId))
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
    }

    private void registrarDiagnosticoCifradoProgramado(MensajeProgramadoEntity scheduled,
                                                       MensajeEntity persisted,
                                                       MensajeDTO dtoEmitido,
                                                       UsuarioEntity emisor,
                                                       UsuarioEntity receptor,
                                                       String payloadJson,
                                                       String rsaRuntimeAlgorithm) {
        if (scheduled == null || persisted == null) {
            return;
        }
        try {
            JsonNode root = payloadJson == null ? null : OBJECT_MAPPER.readTree(payloadJson);
            String forEmisor = textField(root, "forEmisor");
            String forReceptor = textField(root, "forReceptor");
            String forAdmin = textField(root, "forAdmin");
            String iv = textField(root, "iv");
            String ciphertext = textField(root, "ciphertext");
            String payloadType = textField(root, "type");
            String contenidoPersistido = persisted.getContenido();
            String contenidoEmitido = dtoEmitido == null ? null : dtoEmitido.getContenido();

            LOGGER.info("[SCHEDULED_MESSAGE_E2E_DIAG] scheduledMessageId={} mensajeId={} chatId={} payloadType={} emisorId={} receptorId={} emisorKeyFp={} receptorKeyFp={} forEmisorHash={} forReceptorHash={} forAdminHash={} ivLenB64={} ivLenBytes={} ciphertextLenB64={} ciphertextLenBytes={} forEmisorLenB64={} forEmisorLenBytes={} forReceptorLenB64={} forReceptorLenBytes={} forAdminLenB64={} forAdminLenBytes={} rsaRuntime={} persistedContentHash={} emittedContentHash={} persistedEqualsEmitted={}",
                    scheduled.getId(),
                    persisted.getId(),
                    persisted.getChat() == null ? null : persisted.getChat().getId(),
                    payloadType,
                    emisor == null ? null : emisor.getId(),
                    receptor == null ? null : receptor.getId(),
                    E2EDiagnosticUtils.fingerprint12(emisor == null ? null : emisor.getPublicKey()),
                    E2EDiagnosticUtils.fingerprint12(receptor == null ? null : receptor.getPublicKey()),
                    E2EDiagnosticUtils.fingerprint12(forEmisor),
                    E2EDiagnosticUtils.fingerprint12(forReceptor),
                    E2EDiagnosticUtils.fingerprint12(forAdmin),
                    safeLen(iv),
                    base64DecodedLen(iv),
                    safeLen(ciphertext),
                    base64DecodedLen(ciphertext),
                    safeLen(forEmisor),
                    base64DecodedLen(forEmisor),
                    safeLen(forReceptor),
                    base64DecodedLen(forReceptor),
                    safeLen(forAdmin),
                    base64DecodedLen(forAdmin),
                    rsaRuntimeAlgorithm,
                    E2EDiagnosticUtils.fingerprint12(contenidoPersistido),
                    E2EDiagnosticUtils.fingerprint12(contenidoEmitido),
                    Objects.equals(contenidoPersistido, contenidoEmitido));
        } catch (Exception ex) {
            LOGGER.warn("[SCHEDULED_MESSAGE_E2E_DIAG_WARN] scheduledMessageId={} mensajeId={} errorType={}",
                    scheduled.getId(),
                    persisted.getId(),
                    ex.getClass().getSimpleName());
        }
    }

    private String textField(JsonNode root, String field) {
        if (root == null || field == null || field.isBlank()) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Integer safeLen(String value) {
        return value == null ? null : value.length();
    }

    private Integer base64DecodedLen(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(value).length;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ValidatedE2EPayload validarPayloadE2EProgramadoOrThrow(String rawPayload,
                                                                   Long authUserId,
                                                                   List<Long> chatIds,
                                                                   Instant scheduledAt) {
        ValidatedE2EPayload parsed = parsePayloadE2EProgramado(rawPayload);
        if (parsed != null) {
            return parsed;
        }
        String code = looksLikeFilePayload(rawPayload)
                ? Constantes.ERR_E2E_FILE_PAYLOAD_INVALID
                : Constantes.ERR_E2E_PAYLOAD_INVALID;
        String detail = "contenido no cumple esquema E2E/E2E_GROUP/E2E_FILE/E2E_GROUP_FILE requerido";
        String traceId = E2EDiagnosticUtils.newTraceId();
        LOGGER.warn("[SCHEDULED_MESSAGE_CREATE] userId={} chatIds={} scheduledAtUTC={} hasContenidoE2E=true error={} detail={}",
                authUserId,
                chatIds,
                scheduledAt,
                code,
                detail);
        LOGGER.warn("[VALIDATION_ERROR] code={} traceId={} tipo={} source=SCHEDULED_MESSAGE_CREATE",
                code,
                traceId,
                looksLikeFilePayload(rawPayload) ? Constantes.TIPO_FILE : Constantes.TIPO_TEXT);
        throw new E2EGroupValidationException(code, detail + " traceId=" + traceId);
    }

    private void validarCompatibilidadTipoE2EConChat(String payloadType,
                                                     ChatEntity chat,
                                                     Long authUserId,
                                                     Set<Long> chatIds,
                                                     Instant scheduledAt) {
        if (payloadType == null || chat == null) {
            return;
        }
        Long chatId = chat.getId();
        boolean isIndividual = chat instanceof ChatIndividualEntity
                || (chatId != null && chatIndividualRepository.findById(chatId).isPresent());
        boolean isGroup = chat instanceof ChatGrupalEntity
                || (chatId != null && chatGrupalRepository.findById(chatId).isPresent());
        boolean compatible = (isIndividual && (TYPE_E2E.equals(payloadType) || TYPE_E2E_FILE.equals(payloadType)))
                || (isGroup && (TYPE_E2E_GROUP.equals(payloadType) || TYPE_E2E_GROUP_FILE.equals(payloadType)));
        if (compatible) {
            return;
        }
        String tipoChat = isGroup ? "GRUPAL" : "INDIVIDUAL";
        String detail = "contenido type=" + payloadType + " no compatible con chatId=" + chatId + " tipoChat=" + tipoChat;
        String code = isFilePayloadType(payloadType)
                ? Constantes.ERR_E2E_FILE_PAYLOAD_INVALID
                : Constantes.ERR_E2E_PAYLOAD_INVALID;
        String traceId = E2EDiagnosticUtils.newTraceId();
        LOGGER.warn("[SCHEDULED_MESSAGE_CREATE] userId={} chatIds={} scheduledAtUTC={} hasContenidoE2E=true error={} detail={}",
                authUserId,
                chatIds,
                scheduledAt,
                code,
                detail);
        LOGGER.warn("[VALIDATION_ERROR] code={} traceId={} tipo={} source=SCHEDULED_CHAT_TYPE_COMPAT",
                code,
                traceId,
                isFilePayloadType(payloadType) ? Constantes.TIPO_FILE : Constantes.TIPO_TEXT);
        throw new E2EGroupValidationException(code, detail + " traceId=" + traceId);
    }

    private ValidatedE2EPayload parsePayloadE2EProgramado(String rawPayload) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawPayload);
            String type = requiredTextField(root, "type");
            if (!TYPE_E2E.equals(type)
                    && !TYPE_E2E_GROUP.equals(type)
                    && !TYPE_E2E_FILE.equals(type)
                    && !TYPE_E2E_GROUP_FILE.equals(type)) {
                return null;
            }
            if (TYPE_E2E.equals(type) || TYPE_E2E_GROUP.equals(type)) {
                if (requiredTextField(root, "iv") == null
                        || requiredTextField(root, "ciphertext") == null
                        || requiredTextField(root, "forEmisor") == null
                        || requiredTextField(root, "forAdmin") == null) {
                    return null;
                }
            } else {
                if (requiredTextField(root, "ivFile") == null
                        || requiredTextField(root, "fileUrl") == null
                        || requiredTextField(root, "fileMime") == null
                        || requiredTextField(root, "fileNombre") == null
                        || requiredTextField(root, "forEmisor") == null
                        || requiredTextField(root, "forAdmin") == null) {
                    return null;
                }
                Long fileSizeBytes = requiredLongField(root, "fileSizeBytes");
                if (fileSizeBytes == null || fileSizeBytes <= 0L || fileSizeBytes > maxUploadFileBytes) {
                    return null;
                }
            }
            if (TYPE_E2E.equals(type) || TYPE_E2E_FILE.equals(type)) {
                if (requiredTextField(root, "forReceptor") == null) {
                    return null;
                }
            } else {
                JsonNode forReceptoresNode = root.get("forReceptores");
                if (forReceptoresNode == null || !forReceptoresNode.isObject() || !forReceptoresNode.fields().hasNext()) {
                    return null;
                }
            }
            return new ValidatedE2EPayload(type);
        } catch (Exception ex) {
            return null;
        }
    }

    private String requiredTextField(JsonNode root, String field) {
        if (root == null || field == null || field.isBlank()) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    private Long requiredLongField(JsonNode root, String field) {
        if (root == null || field == null || field.isBlank()) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private String resolverModoProcesamiento(String messageContent) {
        return parsePayloadE2EProgramado(messageContent) == null
                ? MODE_LEGACY_ENCRYPT
                : MODE_PASSTHROUGH_E2E;
    }

    private boolean isFilePayloadType(String type) {
        return TYPE_E2E_FILE.equals(type) || TYPE_E2E_GROUP_FILE.equals(type);
    }

    private boolean looksLikeFilePayload(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return false;
        }
        return rawPayload.contains(TYPE_E2E_FILE) || rawPayload.contains(TYPE_E2E_GROUP_FILE);
    }

    private String resolverTipoMensajeProgramado(String messageContent) {
        ValidatedE2EPayload parsed = parsePayloadE2EProgramado(messageContent);
        if (parsed == null) {
            return Constantes.TIPO_TEXT;
        }
        return isFilePayloadType(parsed.type()) ? Constantes.TIPO_FILE : Constantes.TIPO_TEXT;
    }

    private List<ScheduledEmailRecipient> normalizeScheduledRecipients(List<ScheduledEmailRecipient> recipients) {
        if (recipients == null) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<ScheduledEmailRecipient> out = new ArrayList<>();
        for (ScheduledEmailRecipient recipient : recipients) {
            if (recipient == null || !StringUtils.hasText(recipient.email()) || !isValidEmail(recipient.email())) {
                continue;
            }
            String email = recipient.email().trim().toLowerCase(Locale.ROOT);
            if (seen.add(email)) {
                out.add(new ScheduledEmailRecipient(recipient.userId(), email));
            }
        }
        return out;
    }

    private List<ScheduledRecipientUserDTO> buildRecipientUsers(List<UsuarioEntity> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream().map(this::toRecipientUser).filter(Objects::nonNull).toList();
    }

    private List<ScheduledRecipientUserDTO> buildRecipientUsersFromScheduledRecipients(List<ScheduledEmailRecipient> recipients) {
        if (recipients == null) {
            return List.of();
        }
        return recipients.stream()
                .map(recipient -> {
                    UsuarioEntity user = recipient.userId() == null ? null : usuarioRepository.findById(recipient.userId()).orElse(null);
                    return user != null ? toRecipientUser(user) : toRecipientUser(recipient.userId(), null, recipient.email(), null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private ScheduledRecipientUserDTO toRecipientUser(UsuarioEntity user) {
        if (user == null) {
            return null;
        }
        return toRecipientUser(user.getId(), null, user.getEmail(), buildFullName(user));
    }

    private ScheduledRecipientUserDTO toRecipientUser(Long userId, Long chatId, String email, String fullName) {
        ScheduledRecipientUserDTO dto = new ScheduledRecipientUserDTO();
        dto.setUserId(userId);
        dto.setChatId(chatId);
        dto.setEmail(StringUtils.hasText(email) ? email.trim() : null);
        dto.setFullName(StringUtils.hasText(fullName) ? fullName.trim() : null);
        return dto;
    }

    private String buildFullName(UsuarioEntity user) {
        if (user == null) {
            return null;
        }
        String nombre = user.getNombre() == null ? "" : user.getNombre().trim();
        String apellido = user.getApellido() == null ? "" : user.getApellido().trim();
        String fullName = (nombre + " " + apellido).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private List<Long> collectUserIds(List<ScheduledRecipientUserDTO> recipientUsers) {
        if (recipientUsers == null) {
            return List.of();
        }
        return recipientUsers.stream()
                .map(ScheduledRecipientUserDTO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> collectRecipientEmails(List<ScheduledRecipientUserDTO> recipientUsers) {
        if (recipientUsers == null) {
            return List.of();
        }
        return recipientUsers.stream()
                .map(ScheduledRecipientUserDTO::getEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String serializeAdminPayload(AdminScheduledPayload payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar metadata administrativa programada", ex);
        }
    }

    private AdminScheduledPayload parseAdminPayload(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(raw, AdminScheduledPayload.class);
        } catch (Exception ex) {
            LOGGER.warn("[SCHEDULED_ADMIN_PAYLOAD_PARSE_WARN] errorType={}", ex.getClass().getSimpleName());
            return null;
        }
    }

    private List<ScheduledAttachmentMetaDTO> readAttachmentMeta(String attachmentPayload) {
        if (!StringUtils.hasText(attachmentPayload)) {
            return List.of();
        }
        try {
            List<StoredEmailAttachment> stored = OBJECT_MAPPER.readValue(attachmentPayload, new TypeReference<List<StoredEmailAttachment>>() {
            });
            List<ScheduledAttachmentMetaDTO> out = new ArrayList<>();
            for (StoredEmailAttachment item : stored) {
                if (item == null) {
                    continue;
                }
                ScheduledAttachmentMetaDTO dto = new ScheduledAttachmentMetaDTO();
                dto.setFileName(item.fileName());
                dto.setMimeType(item.mimeType());
                dto.setSizeBytes(item.sizeBytes());
                out.add(dto);
            }
            return out;
        } catch (Exception ex) {
            LOGGER.warn("[SCHEDULED_ATTACHMENT_META_PARSE_WARN] errorType={}", ex.getClass().getSimpleName());
            return List.of();
        }
    }

    private List<EmailAttachmentDTO> loadScheduledEmailAttachments(String attachmentPayload) {
        if (!StringUtils.hasText(attachmentPayload)) {
            return List.of();
        }
        try {
            StoredEmailAttachment[] stored = OBJECT_MAPPER.readValue(attachmentPayload, StoredEmailAttachment[].class);
            List<EmailAttachmentDTO> attachments = new ArrayList<>();
            Path scheduledRoot = Paths.get(uploadsRoot).toAbsolutePath().normalize().resolve(SCHEDULED_ATTACHMENTS_DIR);
            for (StoredEmailAttachment item : stored) {
                if (item == null || !StringUtils.hasText(item.storageKey())) {
                    continue;
                }
                Path path = resolveStoredAttachmentPath(scheduledRoot, item.storageKey());
                if (!Files.exists(path)) {
                    throw new IllegalStateException("Adjunto programado no encontrado: " + item.fileName());
                }
                EmailAttachmentDTO dto = new EmailAttachmentDTO();
                dto.setFileName(item.fileName());
                dto.setMimeType(item.mimeType());
                dto.setContent(Files.readAllBytes(path));
                attachments.add(dto);
            }
            return attachments;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron cargar adjuntos programados", ex);
        }
    }

    private Path resolveStoredAttachmentPath(Path scheduledRoot, String storageKey) {
        String normalizedKey = storageKey == null ? "" : storageKey.replace("\\", "/").trim();
        Path candidate;
        if (normalizedKey.contains(":") || normalizedKey.startsWith("/")) {
            candidate = Paths.get(normalizedKey).toAbsolutePath().normalize();
        } else {
            candidate = scheduledRoot.resolve(normalizedKey).normalize();
        }
        if (!candidate.startsWith(scheduledRoot)) {
            throw new IllegalArgumentException("Adjunto programado con ruta invalida");
        }
        return candidate;
    }

    private String sanitizeAttachmentMimeType(String mimeType) {
        String normalized = StringUtils.hasText(mimeType) ? mimeType.trim().toLowerCase(Locale.ROOT) : Constantes.MIME_APPLICATION_OCTET_STREAM;
        if (!ALLOWED_ATTACHMENT_MIME_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("mimeType de adjunto no permitido");
        }
        return normalized;
    }

    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.length() <= 320 && EMAIL_PATTERN.matcher(normalized).matches();
    }

    private String normalizeAudienceMode(String audienceMode) {
        return audienceMode == null ? "" : audienceMode.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizePlainText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private String sanitizeBodyText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace("\u0000", "").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private String sanitizeEmailVariable(String value) {
        return value == null ? "" : value.replace("\u0000", "").trim();
    }

    private String sanitizeEmailBody(String body) {
        if (body == null) {
            return "";
        }
        return body.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\r\n", "<br/>")
                .replace("\n", "<br/>");
    }

    private boolean esErrorNoRecuperable(Exception ex) {
        if (ex instanceof ExcepcionCifradoProgramado excepcionCifradoProgramado) {
            return !excepcionCifradoProgramado.isRecuperable();
        }
        return ex instanceof AccessDeniedException
                || ex instanceof ChatCerradoException
                || ex instanceof RecursoNoEncontradoException
                || ex instanceof IllegalArgumentException;
    }

    private String truncarError(String error) {
        if (error == null) {
            return null;
        }
        String normalized = error.replaceAll("[\\r\\n\\t]+", " ").trim();
        normalized = normalized.replaceAll("(?i)select\\s+.+", "internal error");
        normalized = normalized.replaceAll("(?i)insert\\s+.+", "internal error");
        normalized = normalized.replaceAll("(?i)update\\s+.+", "internal error");
        normalized = normalized.replaceAll("(?i)delete\\s+.+", "internal error");
        normalized = normalized.replaceAll("(?i)password=[^\\s]+", "password=***");
        normalized = normalized.replaceAll("(?i)token=[^\\s]+", "token=***");
        normalized = normalized.replaceAll("(?i)secret=[^\\s]+", "secret=***");
        if (normalized.length() <= 1000) {
            return normalized;
        }
        return normalized.substring(0, 1000);
    }

    public String nowUtcIso() {
        return Instant.now().atOffset(ZoneOffset.UTC).toString();
    }

    private void programarEmisionWsAfterCommit(MensajeProgramadoEntity scheduled,
                                               MensajeDTO mensaje,
                                               String e2eType,
                                               String mode,
                                               EstadoMensajeProgramado estadoAnterior,
                                               int intentoActual) {
        Long scheduledId = scheduled == null ? null : scheduled.getId();
        Long chatId = (scheduled == null || scheduled.getChat() == null) ? null : scheduled.getChat().getId();
        Long senderId = mensaje == null ? null : mensaje.getEmisorId();
        Long mensajePersistidoId = mensaje == null ? null : mensaje.getId();

        Runnable emision = () -> {
            try {
                MensajeDTO mensajePersistidoParaEmitir = reconstruirMensajeDesdePersistencia(mensajePersistidoId, mensaje);
                WsEmissionResult result = emitirMensajeRealtime(chatId, mensajePersistidoParaEmitir);
                Instant nowUtc = Instant.now();
                actualizarResultadoFinal(
                        scheduledId,
                        EstadoMensajeProgramado.SENT,
                        nowUtc,
                        null,
                        true,
                        nowUtc,
                        result.destinationsCsv(),
                        null);
                LOGGER.info("[SCHEDULED_MESSAGE_WS_EMIT] scheduledMessageId={} chatId={} senderId={} tipoChat={} e2eType={} wsDestinations={} mensajeIdPersistido={} tsUTC={}",
                        scheduledId,
                        chatId,
                        mensajePersistidoParaEmitir == null ? senderId : mensajePersistidoParaEmitir.getEmisorId(),
                        result.chatType(),
                        e2eType,
                        result.destinationsCsv(),
                        mensajePersistidoId,
                        nowUtc);
                LOGGER.info("[SCHEDULED_MESSAGE_ITEM] id={} chatId={} {}->{} attempts={} mode={} tipo={} mensajeIdPersistido={} e2eType={} wsDestinations={} sentAtUTC={}",
                        scheduledId,
                        chatId,
                        estadoAnterior,
                        EstadoMensajeProgramado.SENT,
                        intentoActual,
                        mode,
                        mensajePersistidoParaEmitir == null ? null : mensajePersistidoParaEmitir.getTipo(),
                        mensajePersistidoId,
                        e2eType,
                        result.destinationsCsv(),
                        nowUtc);
            } catch (Exception ex) {
                Instant nowUtc = Instant.now();
                String err = truncarError(ex.getMessage());
                boolean agotado = intentoActual >= Math.max(1, maxAttempts);
                EstadoMensajeProgramado nuevoEstado = agotado
                        ? EstadoMensajeProgramado.FAILED
                        : EstadoMensajeProgramado.PENDING;
                actualizarResultadoFinal(
                        scheduledId,
                        nuevoEstado,
                        null,
                        err,
                        false,
                        null,
                        null,
                        err);
                LOGGER.warn("[SCHEDULED_MESSAGE_WS_EMIT_FAIL] scheduledMessageId={} chatId={} senderId={} mensajeIdPersistido={} mode={} e2eType={} nextStatus={} attempts={} error={} tsUTC={}",
                        scheduledId,
                        chatId,
                        senderId,
                        mensajePersistidoId,
                        mode,
                        e2eType,
                        nuevoEstado,
                        intentoActual,
                        err,
                        nowUtc);
                LOGGER.error("[SCHEDULED_MESSAGE_WS_EMIT_FAIL_STACK] scheduledMessageId={} chatId={} errorType={}",
                        scheduledId,
                        chatId,
                        ex.getClass().getSimpleName(),
                        ex);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emision.run();
                }
            });
            return;
        }
        emision.run();
    }

    private MensajeDTO reconstruirMensajeDesdePersistencia(Long mensajeId, MensajeDTO fallback) {
        if (mensajeId == null) {
            return fallback;
        }
        Optional<MensajeEntity> persistedOpt = mensajeRepository.findById(mensajeId);
        if (persistedOpt.isEmpty()) {
            return fallback;
        }
        MensajeEntity persisted = persistedOpt.get();
        MensajeDTO dto = MappingUtils.mensajeEntityADto(persisted);
        if (persisted.getEmisor() != null) {
            dto.setEmisorNombre(persisted.getEmisor().getNombre());
            dto.setEmisorApellido(persisted.getEmisor().getApellido());
            dto.setEmisorFoto(persisted.getEmisor().getFotoUrl());
        }
        return dto;
    }

    private WsEmissionResult emitirMensajeRealtime(Long chatId, MensajeDTO mensaje) {
        if (chatId == null || mensaje == null) {
            return new WsEmissionResult("UNKNOWN", List.of());
        }

        boolean esGrupal = chatGrupalRepository.findById(chatId).isPresent();
        if (esGrupal) {
            String destination = Constantes.TOPIC_CHAT_GRUPAL + chatId;
            messagingTemplate.convertAndSend(destination, mensaje);
            return new WsEmissionResult("GRUPAL", List.of(destination));
        }

        boolean esIndividual = chatIndividualRepository.findById(chatId).isPresent();
        if (esIndividual) {
            List<String> destinations = new ArrayList<>();
            if (mensaje.getEmisorId() != null) {
                String senderDest = Constantes.TOPIC_CHAT + mensaje.getEmisorId();
                messagingTemplate.convertAndSend(senderDest, mensaje);
                destinations.add(senderDest);
            }
            if (mensaje.getReceptorId() != null) {
                String receptorDest = Constantes.TOPIC_CHAT + mensaje.getReceptorId();
                messagingTemplate.convertAndSend(receptorDest, mensaje);
                destinations.add(receptorDest);
            }
            return new WsEmissionResult("INDIVIDUAL", destinations);
        }
        return new WsEmissionResult("UNKNOWN", List.of());
    }

    private void actualizarResultadoFinal(Long scheduledId,
                                          EstadoMensajeProgramado status,
                                          Instant sentAt,
                                          String lastError,
                                          boolean wsEmitted,
                                          Instant wsEmittedAt,
                                          String wsDestinations,
                                          String wsEmitError) {
        if (scheduledId == null) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE chat_scheduled_message " +
                        "SET status = ?, sent_at = ?, last_error = ?, lock_token = NULL, lock_until = NULL, " +
                        "ws_emitted = ?, ws_emitted_at = ?, ws_destinations = ?, ws_emit_error = ?, updated_at = ? " +
                        "WHERE id = ?",
                status == null ? null : status.name(),
                sentAt,
                lastError,
                wsEmitted,
                wsEmittedAt,
                wsDestinations,
                wsEmitError,
                Instant.now(),
                scheduledId);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record PersistenciaProgramadaResultado(MensajeDTO mensaje, String e2eType, String mode) {
    }

    private record ValidatedE2EPayload(String type) {
    }

    private record WsEmissionResult(String chatType, List<String> destinations) {
        String destinationsCsv() {
            if (destinations == null || destinations.isEmpty()) {
                return "";
            }
            return String.join(",", destinations);
        }
    }

    private record ScheduledEmailRecipient(Long userId, String email) {
    }

    private record AdminScheduledPayload(String type,
                                         String audienceMode,
                                         String message,
                                         String subject,
                                         String body,
                                         List<Long> userIds,
                                         List<String> recipientEmails,
                                         @JsonAlias({"recipients", "recipient_users"}) List<ScheduledRecipientUserDTO> recipientUsers,
                                         List<ScheduledAttachmentMetaDTO> attachmentsMeta,
                                         Long expiresAfterReadSeconds) {
    }

    private record StoredEmailAttachment(String fileName, String mimeType, String storageKey, Long sizeBytes) {
    }
}
