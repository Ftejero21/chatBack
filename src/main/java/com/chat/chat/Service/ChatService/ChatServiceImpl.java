package com.chat.chat.Service.ChatService;

import com.chat.chat.DTO.*;
import com.chat.chat.Entity.*;
import com.chat.chat.Mapper.ChatPinnedMessageMapper;
import com.chat.chat.Mapper.GroupMediaItemMapper;
import com.chat.chat.Mapper.GroupMediaMeta;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Exceptions.ValidacionPayloadException;
import com.chat.chat.Repository.*;
import com.chat.chat.Service.EncuestaService.EncuestaService;
import com.chat.chat.Service.EmailService.EmailService;
import com.chat.chat.Utils.*;
import com.chat.chat.Utils.ExceptionConstants;
import com.chat.chat.Utils.ChatConstants;
import com.chat.chat.Configuracion.EstadoUsuarioManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final int DEFAULT_MESSAGES_PAGE = 0;
    private static final int DEFAULT_MESSAGES_SIZE = 50;
    private static final int MAX_MESSAGES_SIZE = 50;
    private static final int DEFAULT_MEDIA_SIZE = 30;
    private static final int MAX_MEDIA_SIZE = 50;
    private static final int DEFAULT_SEARCH_PAGE = 0;
    private static final int DEFAULT_SEARCH_SIZE = 20;
    private static final int MAX_SEARCH_SIZE = 100;
    private static final int DEFAULT_ADMIN_GROUPS_PAGE = 0;
    private static final int DEFAULT_ADMIN_GROUPS_SIZE = 10;
    private static final int MAX_ADMIN_GROUPS_SIZE = 50;
    private static final int MAX_GROUP_NAME_LENGTH = 120;
    private static final int MAX_GROUP_DESCRIPTION_LENGTH = 500;
    private static final int SEARCH_SNIPPET_CONTEXT_CHARS = 40;
    private static final long PIN_DURATION_24H_SECONDS = 86400L;
    private static final long PIN_DURATION_7D_SECONDS = 604800L;
    private static final long PIN_DURATION_30D_SECONDS = 2592000L;
    private static final long MUTE_DURATION_8H_SECONDS = 28800L;
    private static final long MUTE_DURATION_1W_SECONDS = 604800L;
    private static final long DEFAULT_ADMIN_DIRECT_EXPIRES_AFTER_READ_SECONDS = 60L;
    private static final int MAX_CLOSE_REASON_LENGTH = 500;
    private static final String DEFAULT_CLOSE_REASON = "Chat cerrado por un administrador";
    private static final String CURSOR_SEPARATOR = "_";
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern UNSAFE_CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private ChatIndividualRepository chatIndRepo;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Value(Constantes.PROP_UPLOADS_ROOT)
    private String uploadsRoot;

    @Value(Constantes.PROP_UPLOADS_BASE_URL)
    private String uploadsBaseUrl;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private MensajeReaccionRepository mensajeReaccionRepository;

    @Autowired
    private MensajeTemporalAuditoriaRepository mensajeTemporalAuditoriaRepository;

    @Autowired
    private ChatPinnedMessageRepository chatPinnedMessageRepository;

    @Autowired
    private UserPinnedChatRepository userPinnedChatRepository;

    @Autowired
    private UserComplaintRepository userComplaintRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private GroupInviteRepo inviteRepo;
    @Autowired
    private NotificationRepo notificationRepo;

    @Autowired
    private ChatGrupalRepository chatGrupalRepo;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private EncuestaService encuestaService;

    @Autowired
    private ChatPinnedMessageMapper chatPinnedMessageMapper;

    @Autowired
    private GroupMediaItemMapper groupMediaItemMapper;

    @Autowired
    private EstadoUsuarioManager estadoUsuarioManager;

    @Autowired
    private ChatUserStateService chatUserStateService;

    @Autowired
    private EmailService emailService;

    @Override
    public ChatIndividualDTO crearChatIndividual(Long usuario1Id, Long usuario2Id) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        // Solo podemos crear chats donde participemos nosotros mismos
        if (!authenticatedUserId.equals(usuario1Id) && !authenticatedUserId.equals(usuario2Id)) {
            throw new SecurityException(ExceptionConstants.ERROR_CREATE_THIRD_PARTY_CHAT);
        }

        UsuarioEntity u1 = usuarioRepo.findById(usuario1Id).orElseThrow();
        UsuarioEntity u2 = usuarioRepo.findById(usuario2Id).orElseThrow();

        if (u1.getBloqueados().contains(u2) || u2.getBloqueados().contains(u1)) {
            throw new RuntimeException(Constantes.MSG_CHAT_INDIVIDUAL_BLOQUEADO);
        }

        ChatIndividualEntity chat = chatIndRepo.findRegularChatBetween(u1, u2)
                .orElseGet(() -> {
                    ChatIndividualEntity nuevo = new ChatIndividualEntity();
                    nuevo.setUsuario1(u1);
                    nuevo.setUsuario2(u2);
                    nuevo.setAdminDirect(false);
                    return chatIndRepo.save(nuevo);
                });

        return MappingUtils.chatIndividualEntityADto(chat);
    }

    @Override
    @Transactional
    public AdminDirectMessageResponseDTO enviarMensajeDirectoAdmin(AdminDirectMessageRequestDTO request) {
        validateAdminOnly();
        if (request == null) {
            throw new IllegalArgumentException("request es obligatorio");
        }

        Long adminId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity admin = usuarioRepo.findById(adminId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));

        long expiresAfterReadSeconds = normalizeAdminDirectExpiry(request.getExpiresAfterReadSeconds());
        Map<Long, AdminDirectMessagePayloadDTO> payloadsByUserId = resolveAdminDirectPayloads(request);
        LinkedHashSet<Long> userIds = new LinkedHashSet<>(payloadsByUserId.keySet());

        AdminDirectMessageResponseDTO response = new AdminDirectMessageResponseDTO();
        response.setRequestedUsers(userIds.size());
        List<AdminDirectMessageResultDTO> results = new ArrayList<>();

        for (Long userId : userIds) {
            if (Objects.equals(userId, adminId)) {
                throw new IllegalArgumentException("no se permite admin->admin en este flujo");
            }
            UsuarioEntity receptor = usuarioRepo.findById(userId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_EXISTE_ID + userId));
            AdminDirectMessagePayloadDTO payload = payloadsByUserId.get(userId);
            String contenido = payload == null ? null : payload.getContenido();
            if (contenido == null || contenido.isBlank()) {
                throw new IllegalArgumentException("contenido es obligatorio para userId=" + userId);
            }

            ChatIndividualEntity chat = resolveOrCreateAdminDirectChat(admin, receptor, payload == null ? null : payload.getChatId());
            MensajeEntity saved = persistAdminDirectMessage(admin, receptor, chat, contenido, expiresAfterReadSeconds);
            MensajeDTO dto = MappingUtils.mensajeEntityADto(saved);
            dto.setReceptorId(receptor.getId());

            messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT + receptor.getId(), dto);
            messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT + admin.getId(), dto);

            AdminDirectMessageResultDTO item = new AdminDirectMessageResultDTO();
            item.setUserId(userId);
            item.setChatId(chat.getId());
            item.setMessageId(saved.getId());
            item.setDelivered(true);
            results.add(item);
        }

        response.setResults(results);
        response.setDeliveredUsers(results.size());
        return response;
    }

    private Map<Long, AdminDirectMessagePayloadDTO> resolveAdminDirectPayloads(AdminDirectMessageRequestDTO request) {
        Map<Long, AdminDirectMessagePayloadDTO> payloadsByUserId = new LinkedHashMap<>();
        List<AdminDirectMessagePayloadDTO> encryptedPayloads = request.getEncryptedPayloads() == null
                ? List.of()
                : request.getEncryptedPayloads();

        if (!encryptedPayloads.isEmpty()) {
            for (AdminDirectMessagePayloadDTO payload : encryptedPayloads) {
                if (payload == null) {
                    throw new IllegalArgumentException("encryptedPayloads contiene item null");
                }
                if (payload.getUserId() == null || payload.getUserId() <= 0) {
                    throw new IllegalArgumentException("encryptedPayloads.userId es obligatorio");
                }
                if (!StringUtils.hasText(payload.getContenido())) {
                    throw new IllegalArgumentException("encryptedPayloads.contenido es obligatorio para userId=" + payload.getUserId());
                }
                if (payloadsByUserId.putIfAbsent(payload.getUserId(), payload) != null) {
                    throw new IllegalArgumentException("encryptedPayloads duplicado para userId=" + payload.getUserId());
                }
            }
            return payloadsByUserId;
        }

        if (!StringUtils.hasText(request.getContenido())) {
            throw new IllegalArgumentException("encryptedPayloads es obligatorio");
        }
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException("userIds es obligatorio");
        }
        for (Long userId : request.getUserIds()) {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("userIds contiene userId invalido");
            }
            AdminDirectMessagePayloadDTO payload = new AdminDirectMessagePayloadDTO();
            payload.setUserId(userId);
            payload.setContenido(request.getContenido());
            if (payloadsByUserId.putIfAbsent(userId, payload) != null) {
                throw new IllegalArgumentException("userIds duplicado: " + userId);
            }
        }
        return payloadsByUserId;
    }

    @Override
    @Transactional
    public BulkEmailResponseDTO enviarBulkEmailAdmin(BulkEmailRequestDTO request, List<MultipartFile> attachments) {
        validateAdminOnly();
        validateBulkEmailRequest(request, attachments);

        List<String> recipientEmails = request.getRecipientEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        List<Long> userIds = request.getUserIds() == null ? List.of() : request.getUserIds();
        List<MultipartFile> safeAttachments = attachments == null ? List.of() : attachments.stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .toList();

        BulkEmailResponseDTO response = new BulkEmailResponseDTO();
        List<BulkEmailItemResponseDTO> items = new ArrayList<>();
        int sentCount = 0;

        for (int i = 0; i < recipientEmails.size(); i++) {
            String email = recipientEmails.get(i);
            Long userId = i < userIds.size() ? userIds.get(i) : null;

            BulkEmailItemResponseDTO item = new BulkEmailItemResponseDTO();
            item.setUserId(userId);
            item.setEmail(email);

            try {
                Map<String, String> variables = new HashMap<>();
                variables.put(Constantes.EMAIL_VAR_SUBJECT, sanitizeEmailVariable(request.getSubject()));
                variables.put(Constantes.EMAIL_VAR_BODY, sanitizeEmailBody(request.getBody()));
                emailService.sendHtmlEmailOrThrow(
                        email,
                        request.getSubject().trim(),
                        Constantes.EMAIL_TEMPLATE_ADMIN_BULK,
                        variables,
                        safeAttachments);
                item.setOk(true);
                item.setStatus("SENT");
                item.setError(null);
                sentCount++;
                LOGGER.info("[ADMIN_BULK_EMAIL] status=SENT email={} userId={} attachments={}", email, userId, safeAttachments.size());
            } catch (Exception ex) {
                item.setOk(false);
                item.setStatus("FAILED");
                item.setError(resolveSafeBulkEmailError(ex));
                LOGGER.error("[ADMIN_BULK_EMAIL] status=FAILED email={} userId={} attachments={} error={}",
                        email,
                        userId,
                        safeAttachments.size(),
                        ex.getMessage(),
                        ex);
            }
            items.add(item);
        }

        response.setOk(true);
        response.setSentCount(sentCount);
        response.setFailedCount(items.size() - sentCount);
        response.setItems(items);
        response.setMessage("Bulk email processed");
        return response;
    }

    @Override
    @Transactional
    public ChatGrupalDTO crearChatGrupal(ChatGrupalDTO dto) {
        // En vez de usar el ID que manda el front (que se puede falsificar), usamos el
        // del token
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();

        // 1) Creador desde auth
        UsuarioEntity creador = usuarioRepo.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CREADOR_NO_ENCONTRADO));

        // 2) Procesar fotoGrupo: dataURL -> guardar; URL pÃºblica -> usar
        String fotoUrl = null;
        if (dto.getFotoGrupo() != null) {
            String f = dto.getFotoGrupo().trim();
            if (f.startsWith(Constantes.DATA_IMAGE_PREFIX)) {
                fotoUrl = Utils.saveDataUrlToUploads(f, Constantes.DIR_GROUP_PHOTOS, uploadsRoot, uploadsBaseUrl);
            } else if (Utils.isPublicUrl(f)) {
                fotoUrl = f;
            }
        }

        // 3) Crear el chat con el creador como Ãºnico miembro inicial
        ChatGrupalEntity chat = new ChatGrupalEntity();
        chat.setNombreGrupo(dto.getNombreGrupo());
        chat.setFotoUrl(fotoUrl);
        chat.setDescripcion(dto.getDescripcion());
        if (dto.getVisibilidad() != null && !dto.getVisibilidad().isBlank()) {
            try {
                chat.setVisibilidad(GroupVisibility.valueOf(dto.getVisibilidad().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                chat.setVisibilidad(GroupVisibility.PRIVADO);
            }
        } else {
            chat.setVisibilidad(GroupVisibility.PRIVADO);
        }
        chat.setCreador(creador);
        chat.setUsuarios(new ArrayList<>(List.of(creador)));
        chat.getAdmins().add(creador);
        chat.setMediaCount(0);
        chat.setFilesCount(0);
        chat.setActivo(true);
        chat = chatGrupalRepo.save(chat);

        // 4) Preparar invitados (los que vienen en dto, excepto el creador, sin
        // duplicados)
        Set<Long> inviteeIds = (dto.getUsuarios() == null) ? Set.of()
                : dto.getUsuarios().stream()
                        .map(UsuarioDTO::getId)
                        .filter(id -> !Objects.equals(id, authenticatedUserId))
                        .filter(id -> {
                            UsuarioEntity potentialInvitee = usuarioRepo.findById(id).orElse(null);
                            if (potentialInvitee == null)
                                return false;
                            // Prevenir agregar si creador bloqueÃ³ al invitado, o si el invitado bloqueÃ³ al
                            // creador
                            return !creador.getBloqueados().contains(potentialInvitee) &&
                                    !potentialInvitee.getBloqueados().contains(creador);
                        })
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // 5) Crear invitaciones
        List<GroupInviteEntity> invites = new ArrayList<>();
        for (Long inviteeId : inviteeIds) {
            UsuarioEntity invitee = usuarioRepo.findById(inviteeId)
                    .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_USUARIO_INVITADO_NO_EXISTE + inviteeId));
            GroupInviteEntity inv = new GroupInviteEntity();
            inv.setChat(chat);
            inv.setInviter(creador);
            inv.setInvitee(invitee);
            inv.setStatus(InviteStatus.PENDING);
            invites.add(inv);
        }
        inviteRepo.saveAll(invites);

        // 6) Guardar notificaciÃ³n + enviar WS a cada invitado
        for (GroupInviteEntity inv : invites) {
            GroupInviteWS wsPayload = new GroupInviteWS();
            wsPayload.inviteId = inv.getId();
            wsPayload.groupId = chat.getId();
            wsPayload.groupName = chat.getNombreGrupo();
            wsPayload.inviterId = creador.getId();
            wsPayload.inviterNombre = creador.getNombre();

            NotificationEntity notif = new NotificationEntity();
            notif.setUserId(inv.getInvitee().getId());
            notif.setType(NotificationType.GROUP_INVITE);
            notif.setPayloadJson(Utils.writeJson(wsPayload));
            notif.setSeen(false);
            notificationRepo.save(notif);

            long unseen = notificationRepo.countByUserIdAndSeenFalse(inv.getInvitee().getId());
            wsPayload.unseenCount = (int) unseen;

            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_NOTIFICATIONS + inv.getInvitee().getId(), wsPayload);
        }

        // 7) DTO de salida (con foto)
        ChatGrupalDTO out = MappingUtils.chatGrupalEntityADto(chat);
        out.setUltimaMensaje(ChatConstants.MSG_GRUPO_CREADO);
        out.setUltimaFecha(LocalDateTime.now());
        out.setIdCreador(creador.getId());
        out.setFotoGrupo(fotoUrl);
        return out;
    }

    @Override
    @Transactional
    public MessagueSalirGrupoDTO salirDeChatGrupal(Long groupId, Long userId) {
        // Validaciones bÃ¡sicas (segÃºn tu requerimiento, userId nunca serÃ¡ null)
        if (groupId == null) {
            return new MessagueSalirGrupoDTO(false, Constantes.MSG_GROUP_ID_OBLIGATORIO, null, userId, false);
        }

        ChatGrupalEntity chat = chatGrupalRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_GRUPO_NO_EXISTE_ID + groupId));

        UsuarioEntity usuario = usuarioRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_USUARIO_NO_EXISTE_ID + userId));

        // Comprobar membresÃ­a
        boolean eraMiembro = chat.getUsuarios() != null &&
                chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), userId));
        if (!eraMiembro) {
            return new MessagueSalirGrupoDTO(false, Constantes.MSG_NO_PERTENECE_GRUPO, groupId, userId, false);
        }

        // Eliminar del listado de usuarios
        Iterator<UsuarioEntity> it = chat.getUsuarios().iterator();
        while (it.hasNext()) {
            UsuarioEntity u = it.next();
            if (Objects.equals(u.getId(), userId)) {
                it.remove();
                break;
            }
        }

        // Si queda vacÃ­o â†’ eliminar grupo
        if (chat.getUsuarios() == null || chat.getUsuarios().isEmpty()) {
            chat.setActivo(false);
            chatGrupalRepo.save(chat);

            return new MessagueSalirGrupoDTO(true,
                    Constantes.MSG_SALIO_GRUPO_ELIMINADO,
                    groupId, userId, true);
        }

        // Persistir cambios cuando no se elimina
        chatGrupalRepo.save(chat);

        MensajeDTO systemMessage = crearMensajeSistemaSalidaGrupo(chat, usuario);
        messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT_GRUPAL + groupId, systemMessage);
        LOGGER.info(Constantes.LOG_E2E_GROUP_MEMBER_LEFT_SYSTEM_MESSAGE_BROADCAST,
                groupId,
                userId,
                systemMessage.getId(),
                systemMessage.getTipo(),
                E2EDiagnosticUtils.fingerprint12(systemMessage.getContenido()),
                systemMessage.getContenido() == null ? 0 : systemMessage.getContenido().length());

        return new MessagueSalirGrupoDTO(true, Constantes.MSG_SALIO_GRUPO, groupId, userId, false);
    }

    @Override
    public EsMiembroDTO esMiembroDeChatGrupal(Long groupId, Long userId) {
        Optional<ChatGrupalEntity> opt = chatGrupalRepo.findById(groupId);
        if (opt.isEmpty())
            return new EsMiembroDTO(false, true);
        ChatGrupalEntity chat = opt.get();
        if (!chat.isActivo()) {
            return new EsMiembroDTO(false, true);
        }
        boolean esMiembro = chat.getUsuarios() != null &&
                chat.getUsuarios().stream().anyMatch(u -> u.getId().equals(userId));
        return new EsMiembroDTO(esMiembro, false);
    }

    @Override
    public List<ChatGrupalDTO> listarChatsGrupalesPorUsuario(Long usuarioId) {
        validateSelfOrAdminAuditor(usuarioId);
        UsuarioEntity usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        return chatGrupalRepo.findAll().stream()
                .filter(ChatGrupalEntity::isActivo)
                .filter(chat -> chat.getUsuarios().contains(usuario))
                .map(MappingUtils::chatGrupalEntityADto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AdminGroupListDTO> listarGruposAdmin(Integer page, Integer size) {
        validateAdminOnly();

        int safePage = page == null ? DEFAULT_ADMIN_GROUPS_PAGE : Math.max(0, page);
        int safeSize = size == null ? DEFAULT_ADMIN_GROUPS_SIZE : Math.max(1, Math.min(size, MAX_ADMIN_GROUPS_SIZE));

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "id")));
        return chatGrupalRepo.findAdminGroupPage(pageable);
    }

    @Override
    @Transactional
    public ChatCloseStateDTO cerrarChatGrupalComoAdmin(Long chatId, String motivo, String ip, String userAgent) {
        validateAdminOnly();
        Long adminId = securityUtils.getAuthenticatedUserId();
        String normalizedReason = normalizeCloseReason(motivo, true);
        ChatGrupalEntity chat = chatGrupalRepo.findByIdWithUsuariosForUpdate(chatId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
        if (!chat.isActivo()) {
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId);
        }

        if (!chat.isClosed()) {
            chat.setClosed(true);
            chat.setClosedReason(normalizedReason);
            chat.setClosedAt(Instant.now());
            chat.setClosedByAdminId(adminId);
            chat = chatGrupalRepo.save(chat);
        }

        ChatCloseStateDTO response = toChatCloseState(chat);
        emitChatClosureEvent(chat, response);
        LOGGER.info(
                "[AUDIT_CHAT_GROUP_CLOSE] action=CLOSE adminId={} chatId={} ts={} reason={} ip={} userAgent={}",
                adminId,
                chatId,
                Instant.now(),
                response.getReason(),
                safeAuditValue(ip),
                safeAuditValue(userAgent));
        return response;
    }

    @Override
    @Transactional
    public ChatCloseStateDTO reabrirChatGrupalComoAdmin(Long chatId, String ip, String userAgent) {
        validateAdminOnly();
        Long adminId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = chatGrupalRepo.findByIdWithUsuariosForUpdate(chatId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
        if (!chat.isActivo()) {
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId);
        }

        if (chat.isClosed()) {
            chat.setClosed(false);
            chat.setClosedReason(null);
            chat.setClosedAt(null);
            chat.setClosedByAdminId(null);
            chat = chatGrupalRepo.save(chat);
        }

        ChatCloseStateDTO response = toChatCloseState(chat);
        emitChatClosureEvent(chat, response);
        LOGGER.info(
                "[AUDIT_CHAT_GROUP_CLOSE] action=REOPEN adminId={} chatId={} ts={} reason={} ip={} userAgent={}",
                adminId,
                chatId,
                Instant.now(),
                response.getReason(),
                safeAuditValue(ip),
                safeAuditValue(userAgent));
        return response;
    }

    @Override
    @Transactional
    public AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(AddUsuariosGrupoDTO dto) {
        if (dto == null || dto.getGroupId() == null) {
            throw new IllegalArgumentException(Constantes.MSG_GROUP_ID_OBLIGATORIO);
        }

        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();

        // 1) Validar grupo e invitador
        ChatGrupalEntity chat = chatGrupalRepo.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_CHAT_GRUPAL_NO_EXISTE + dto.getGroupId()));

        UsuarioEntity inviter = usuarioRepo.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_INVITADOR_NO_EXISTE + authenticatedUserId));

        // Comprobar que el invitador pertenece al grupo
        boolean inviterEsMiembro = chat.getUsuarios() != null
                && chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), inviter.getId()));
        if (!inviterEsMiembro) {
            throw new IllegalStateException(ExceptionConstants.ERROR_NOT_GROUP_MEMBER);
        }

        // 2) Normalizar IDs a invitar (sin duplicados, sin incluir al invitador)
        Set<Long> solicitados = (dto.getUsuarios() == null) ? Set.of()
                : dto.getUsuarios().stream()
                        .map(UsuarioDTO::getId)
                        .filter(Objects::nonNull)
                        .filter(id -> !Objects.equals(id, inviter.getId()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3) Filtrar ya miembros
        Set<Long> miembrosIds = chat.getUsuarios() == null ? Set.of()
                : chat.getUsuarios().stream().map(UsuarioEntity::getId).collect(Collectors.toSet());

        Set<Long> noMiembros = solicitados.stream()
                .filter(id -> !miembrosIds.contains(id))
                .filter(id -> {
                    UsuarioEntity potentialInvitee = usuarioRepo.findById(id).orElse(null);
                    if (potentialInvitee == null)
                        return false;
                    // Prevenir agregar si invitador bloqueÃ³ al invitado, o si el invitado bloqueÃ³
                    // al invitador
                    return !inviter.getBloqueados().contains(potentialInvitee) &&
                            !potentialInvitee.getBloqueados().contains(inviter);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 4) Evitar duplicar invitaciones PENDING
        List<GroupInviteEntity> pendientesExistentes = inviteRepo
                .findAllByChatIdAndStatus(chat.getId(), InviteStatus.PENDING);

        Set<Long> yaPendientes = pendientesExistentes.stream()
                .map(inv -> inv.getInvitee().getId())
                .collect(Collectors.toSet());

        List<GroupInviteEntity> nuevasInvitaciones = new ArrayList<>();
        int contadorYaPendientes = 0;

        for (Long inviteeId : noMiembros) {
            if (yaPendientes.contains(inviteeId)) {
                contadorYaPendientes++;
                continue;
            }
            UsuarioEntity invitee = usuarioRepo.findById(inviteeId)
                    .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_USUARIO_INVITADO_NO_EXISTE + inviteeId));

            GroupInviteEntity inv = new GroupInviteEntity();
            inv.setChat(chat);
            inv.setInviter(inviter);
            inv.setInvitee(invitee);
            inv.setStatus(InviteStatus.PENDING);
            inv.setCreatedAt(LocalDateTime.now());
            nuevasInvitaciones.add(inv);
        }

        if (!nuevasInvitaciones.isEmpty()) {
            inviteRepo.saveAll(nuevasInvitaciones);
        }

        // 5) Notificar y emitir WS + construir lista WS para la respuesta
        List<GroupInviteWS> invitacionesWs = new ArrayList<>();
        for (GroupInviteEntity inv : nuevasInvitaciones) {
            GroupInviteWS wsPayload = new GroupInviteWS();
            wsPayload.setInviteId(inv.getId());
            wsPayload.setGroupId(chat.getId());
            wsPayload.setGroupName(chat.getNombreGrupo());
            wsPayload.setInviterId(inviter.getId());
            wsPayload.setInviterNombre(inviter.getNombre()); // aÃ±ade apellidos si quieres

            NotificationEntity notif = new NotificationEntity();
            notif.setUserId(inv.getInvitee().getId());
            notif.setType(NotificationType.GROUP_INVITE);
            notif.setPayloadJson(Utils.writeJson(wsPayload));
            notif.setSeen(false);
            notificationRepo.save(notif);

            long unseen = notificationRepo.countByUserIdAndSeenFalse(inv.getInvitee().getId());
            wsPayload.setUnseenCount((int) unseen);

            // Emitir WS
            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_NOTIFICATIONS + inv.getInvitee().getId(), wsPayload);

            // AÃ±adir a respuesta
            invitacionesWs.add(wsPayload);
        }

        // 6) Resumen de salida (WS-friendly)
        AddUsuariosGrupoWSResponse resp = new AddUsuariosGrupoWSResponse();
        resp.setGroupId(chat.getId());
        resp.setTotalSolicitados(solicitados.size());
        resp.setYaMiembros(solicitados.size() - noMiembros.size());
        resp.setYaInvitadosPendientes(contadorYaPendientes);
        resp.setInvitadosCreados(nuevasInvitaciones.size());
        resp.setInvitaciones(invitacionesWs);

        return resp;
    }

    @Override
    public List<Object> listarTodosLosChatsDeUsuario(Long usuarioId) {
        validateSelfOrAdminAuditor(usuarioId);
        usuarioRepo.findById(usuarioId).orElseThrow();
        UserPinnedChatEntity pinned = userPinnedChatRepository.findById(usuarioId).orElse(null);
        Long pinnedChatId = pinned == null ? null : pinned.getChatId();
        LocalDateTime pinnedAt = pinned == null ? null : pinned.getPinnedAt();

        List<ChatIndividualEntity> individualChats = chatIndRepo.findAllByUsuario1IdOrUsuario2Id(usuarioId, usuarioId);
        List<ChatGrupalEntity> groupChats = chatGrupalRepo.findAllByUsuariosId(usuarioId).stream()
                .filter(ChatGrupalEntity::isActivo)
                .collect(Collectors.toList());
        List<Long> chatIds = new ArrayList<>();
        individualChats.stream().map(ChatEntity::getId).forEach(chatIds::add);
        groupChats.stream().map(ChatEntity::getId).forEach(chatIds::add);
        Map<Long, Long> cutoffByChatId = chatUserStateService.resolveCutoffs(usuarioId, chatIds);

        Map<Long, Long> unreadMap = mensajeRepository.countUnreadByUser(usuarioId)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0], // chatId
                        r -> (Long) r[1] // count
                ));
        Set<Long> denunciadosPorUsuario = userComplaintRepository.findDistinctDenunciadoIdsByDenuncianteId(usuarioId);

        // === INDIVIDUALES ===
        List<ChatIndividualDTO> individuales = individualChats.stream()
                .map(chat -> {
                    // Decide quiÃ©n es el receptor basado en el usuario actual
                    boolean soyUser1 = chat.getUsuario1().getId().equals(usuarioId);
                    UsuarioEntity peer = soyUser1 ? chat.getUsuario2() : chat.getUsuario1();

                    // Si por algÃºn motivo era un chat conmigo mismo, peer = yo mismo
                    ChatIndividualDTO dto = MappingUtils.chatIndividualEntityADto(chat,
                            soyUser1 ? chat.getUsuario1() : chat.getUsuario2());
                    boolean isPinned = pinnedChatId != null && Objects.equals(dto.getId(), pinnedChatId);
                    dto.setIsPinned(isPinned);
                    dto.setPinnedAt(isPinned ? pinnedAt : null);
                    dto.setReceptor(MappingUtils.usuarioEntityADto(peer));

                    // ðŸ‘‡ convertir la foto del receptor a Base64 (dataURL) para el front
                    if (dto.getReceptor() != null && dto.getReceptor().getFoto() != null) {
                        dto.getReceptor().setFoto(Utils.toDataUrlFromUrl(dto.getReceptor().getFoto(), uploadsRoot));

                    }

                    dto.setUnreadCount(unreadMap.getOrDefault(dto.getId(), 0L));
                    dto.setDenunciado(denunciadosPorUsuario.contains(peer.getId()));

                    // Ãšltimo mensaje por chatId
                    Long cutoff = cutoffByChatId.get(dto.getId());
                    MensajeEntity last = mensajeRepository
                            .findTopVisibleByChatIdOrderByFechaEnvioDesc(dto.getId(), cutoff)
                            .orElse(null);
                    if (cutoff != null && last == null) {
                        // Chat ocultado para el usuario y sin mensajes nuevos desde el ocultado.
                        return null;
                    }
                    if (chat.isAdminDirect() && last == null) {
                        return null;
                    }

                    if (last == null || !isVisibleMessageForUser(last)) {
                        dto.setUltimaMensaje(ChatConstants.MSG_SIN_MENSAJES);
                        dto.setUltimaFecha(null);
                        applyLastMessageMeta(dto, null);
                    } else {
                        dto.setUltimaMensaje(buildIndividualPreview(last, usuarioId));
                        applyLastMessageMeta(dto, last);
                    }
                    dto.setAdminDirect(chat.isAdminDirect());
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // === GRUPALES ===
        List<ChatGrupalDTO> grupales = groupChats.stream()
                .map(chat -> {
                    ChatGrupalDTO dto = MappingUtils.chatGrupalEntityADto(chat);
                    boolean isPinned = pinnedChatId != null && Objects.equals(dto.getId(), pinnedChatId);
                    dto.setIsPinned(isPinned);
                    dto.setPinnedAt(isPinned ? pinnedAt : null);

                    // Foto del GRUPO (soporta fotoGrupo o foto, segÃºn tu DTO)
                    String fg = dto.getFotoGrupo();
                    if (fg != null && !fg.startsWith("data:") && fg.startsWith(Constantes.UPLOADS_PREFIX)) {
                        String data = Utils.toDataUrlFromUrl(fg, uploadsRoot);
                        dto.setFotoGrupo(data != null ? data : fg);
                    }
                    String f2 = dto.getFotoGrupo();
                    if (f2 != null && !f2.startsWith("data:") && f2.startsWith(Constantes.UPLOADS_PREFIX)) {
                        String data = Utils.toDataUrlFromUrl(f2, uploadsRoot);
                        dto.setFotoGrupo(data != null ? data : f2);
                    }

                    // Fotos de miembros (opcional)
                    if (dto.getUsuarios() != null) {
                        dto.getUsuarios().forEach(u -> {
                            String fu = u.getFoto();
                            if (fu != null && !fu.startsWith("data:") && fu.startsWith(Constantes.UPLOADS_PREFIX)) {
                                String data = Utils.toDataUrlFromUrl(fu, uploadsRoot);
                                u.setFoto(data != null ? data : fu);
                            }
                        });
                    }

                    // (Opcional) convertir fotos de miembros a Base64
                    if (dto.getUsuarios() != null) {
                        dto.getUsuarios().forEach(u -> {
                            if (u.getFoto() != null) {
                                u.setFoto(Utils.toDataUrlFromUrl(u.getFoto(), uploadsRoot));
                            }
                        });
                    }

                    // Ãšltimo mensaje por chatId
                    Long cutoff = cutoffByChatId.get(dto.getId());
                    MensajeEntity last = mensajeRepository
                            .findTopVisibleByChatIdOrderByFechaEnvioDesc(dto.getId(), cutoff)
                            .orElse(null);
                    if (cutoff != null && last == null) {
                        // Grupo ocultado para el usuario y sin mensajes nuevos desde el ocultado.
                        return null;
                    }

                    if (last == null || !isVisibleMessageForUser(last)) {
                        dto.setUltimaMensaje(ChatConstants.MSG_SIN_MENSAJES);
                        dto.setUltimaFecha(null);
                        applyLastMessageMeta(dto, null);
                    } else {
                        dto.setUltimaMensaje(buildGroupPreview(last, usuarioId));
                        applyLastMessageMeta(dto, last);
                    }
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // === COMBINAR Y ORDENAR POR FECHA DESC ===
        List<Object> resultado = new ArrayList<>();
        resultado.addAll(individuales);
        resultado.addAll(grupales);

        resultado.sort((a, b) -> {
            boolean pinnedA = isPinnedChat(a);
            boolean pinnedB = isPinnedChat(b);
            if (pinnedA != pinnedB) {
                return pinnedA ? -1 : 1;
            }

            LocalDateTime fa = resolveLastMessageDate(a);
            LocalDateTime fb = resolveLastMessageDate(b);

            if (fa == null && fb == null)
                return 0;
            if (fa == null)
                return 1;
            if (fb == null)
                return -1;
            return fb.compareTo(fa); // DESC
        });

        return resultado;
    }

    private boolean isPinnedChat(Object chatDto) {
        if (chatDto instanceof ChatIndividualDTO individual) {
            return Boolean.TRUE.equals(individual.getIsPinned());
        }
        if (chatDto instanceof ChatGrupalDTO grupal) {
            return Boolean.TRUE.equals(grupal.getIsPinned());
        }
        return false;
    }

    private LocalDateTime resolveLastMessageDate(Object chatDto) {
        if (chatDto instanceof ChatIndividualDTO individual) {
            return individual.getUltimaFecha();
        }
        if (chatDto instanceof ChatGrupalDTO grupal) {
            return grupal.getUltimaFecha();
        }
        return null;
    }

    private String buildIndividualPreview(MensajeEntity last, Long viewerId) {
        if (last == null) {
            return ChatConstants.MSG_SIN_MENSAJES;
        }
        if (!isVisibleMessageForUser(last)) {
            return ChatConstants.MSG_SIN_MENSAJES;
        }
        boolean soyYo = last.getEmisor() != null && Objects.equals(last.getEmisor().getId(), viewerId);
        String prefix = soyYo ? "Tu: " : "";
        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        if (tipo == MessageType.IMAGE) {
            return prefix + "Imagen";
        }
        if (tipo == MessageType.AUDIO) {
            GroupMediaMeta meta = extractMediaMeta(last);
            String dur = Utils.mmss(meta.durMs());
            return prefix + "Audio" + (dur.isEmpty() ? "" : " (" + dur + ")");
        }
        if (tipo == MessageType.FILE) {
            GroupMediaMeta meta = extractMediaMeta(last);
            String nombre = meta.fileName() == null || meta.fileName().isBlank() ? "Archivo" : meta.fileName();
            return prefix + "Archivo: " + nombre;
        }
        if (tipo == MessageType.POLL) {
            return prefix + "Encuesta";
        }
        if (isEncryptedPayload(last.getContenido())) {
            return prefix + "Mensaje cifrado";
        }
        return prefix + Utils.truncarSafe(last.getContenido(), 60);
    }

    private String buildGroupPreview(MensajeEntity last, Long viewerId) {
        if (last == null) {
            return ChatConstants.MSG_SIN_MENSAJES;
        }
        if (!isVisibleMessageForUser(last)) {
            return ChatConstants.MSG_SIN_MENSAJES;
        }
        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        if (tipo == MessageType.SYSTEM) {
            String systemPreview = buildGroupSystemPreview(last, viewerId);
            if (systemPreview != null && !systemPreview.isBlank()) {
                return systemPreview;
            }
        }

        Long senderId = last.getEmisor() == null ? null : last.getEmisor().getId();
        String senderName = last.getEmisor() == null ? null : last.getEmisor().getNombre();
        String prefix;
        if (Objects.equals(senderId, viewerId)) {
            prefix = "yo: ";
        } else if (senderName != null && !senderName.isBlank()) {
            prefix = senderName + ": ";
        } else {
            prefix = "usuario: ";
        }

        if (tipo == MessageType.IMAGE) {
            return prefix + "Imagen";
        }
        if (tipo == MessageType.AUDIO) {
            GroupMediaMeta meta = extractMediaMeta(last);
            String dur = Utils.mmss(meta.durMs());
            return prefix + "Audio" + (dur.isEmpty() ? "" : " (" + dur + ")");
        }
        if (tipo == MessageType.FILE) {
            GroupMediaMeta meta = extractMediaMeta(last);
            String nombre = meta.fileName() == null || meta.fileName().isBlank() ? "Archivo" : meta.fileName();
            return prefix + "Archivo: " + nombre;
        }
        if (tipo == MessageType.POLL) {
            return prefix + "Encuesta";
        }
        if (isEncryptedPayload(last.getContenido())) {
            return prefix + "Mensaje cifrado";
        }
        return prefix + Utils.truncarSafe(last.getContenido(), 60);
    }

    private String buildGroupSystemPreview(MensajeEntity last, Long viewerId) {
        GroupMemberExpelledPayload payload = parseGroupMemberExpelledPayload(last);
        if (payload == null) {
            return null;
        }

        if (Objects.equals(viewerId, payload.actorId())) {
            return "Has expulsado a \"" + payload.targetName() + "\" del grupo \"" + payload.groupName() + "\"";
        }
        if (Objects.equals(viewerId, payload.targetId())) {
            return "Has sido expulsado de \"" + payload.groupName() + "\" por \"" + payload.actorName() + "\"";
        }
        return "\"" + payload.actorName() + "\" ha expulsado a \"" + payload.targetName() + "\" del grupo \"" + payload.groupName() + "\"";
    }

    private GroupMemberExpelledPayload parseGroupMemberExpelledPayload(MensajeEntity last) {
        if (last == null || last.getTipo() != MessageType.SYSTEM || last.getContenido() == null || last.getContenido().isBlank()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(last.getContenido());
            if (root == null || !root.isObject()) {
                return null;
            }
            String systemEvent = firstNonBlank(
                    root.path(Constantes.KEY_SYSTEM_EVENT).asText(null),
                    root.path("event").asText(null));
            if (!Constantes.SYSTEM_EVENT_GROUP_MEMBER_EXPELLED.equalsIgnoreCase(systemEvent)
                    && !Constantes.SYSTEM_EVENT_GROUP_MEMBER_REMOVED.equalsIgnoreCase(systemEvent)
                    && !"GROUP_MEMBER_KICKED".equalsIgnoreCase(systemEvent)) {
                return null;
            }

            Long actorId = root.path(Constantes.KEY_EMISOR_ID).isIntegralNumber()
                    ? root.path(Constantes.KEY_EMISOR_ID).asLong()
                    : (last.getEmisor() == null ? null : last.getEmisor().getId());
            Long targetId = root.path(Constantes.KEY_TARGET_USER_ID).isIntegralNumber()
                    ? root.path(Constantes.KEY_TARGET_USER_ID).asLong()
                    : (root.path("removedUserId").isIntegralNumber() ? root.path("removedUserId").asLong() : null);
            String actorName = firstNonBlank(
                    root.path("actorNombreCompleto").asText(null),
                    buildNombreCompleto(
                            root.path(Constantes.KEY_EMISOR_NOMBRE).asText(null),
                            root.path(Constantes.KEY_EMISOR_APELLIDO).asText(null)),
                    buildNombreCompleto(
                            last.getEmisor() == null ? null : last.getEmisor().getNombre(),
                            last.getEmisor() == null ? null : last.getEmisor().getApellido()));
            String targetName = firstNonBlank(
                    root.path("targetUserName").asText(null),
                    root.path("targetNombreCompleto").asText(null),
                    Constantes.LABEL_USUARIO);
            String groupName = firstNonBlank(
                    root.path("groupName").asText(null),
                    root.path("nombreGrupo").asText(null),
                    "grupo");
            return new GroupMemberExpelledPayload(actorId, targetId, actorName, targetName, groupName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isEncryptedPayload(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String classification = E2EDiagnosticUtils.analyze(content).getClassification();
        return classification != null && classification.startsWith("JSON_E2E");
    }

    private boolean isTemporalExpirado(MensajeEntity mensaje) {
        return mensaje != null
                && mensaje.isMensajeTemporal()
                && mensaje.getExpiraEn() != null
                && !mensaje.getExpiraEn().isAfter(LocalDateTime.now());
    }

    private boolean isAdminTemporalExpiradoOculto(MensajeEntity mensaje) {
        return mensaje != null
                && mensaje.isAdminMessage()
                && mensaje.isMensajeTemporal()
                && (isTemporalExpirado(mensaje)
                || "TEMPORAL_EXPIRADO".equalsIgnoreCase(mensaje.getMotivoEliminacion())
                || mensaje.isExpiredByPolicy());
    }

    private boolean isVisibleMessageForUser(MensajeEntity mensaje) {
        if (mensaje == null || !mensaje.isActivo()) {
            return false;
        }
        if (isAdminTemporalExpiradoOculto(mensaje)) {
            return false;
        }
        return !isTemporalExpirado(mensaje);
    }

    private String buildPlaceholderTemporal(MensajeEntity mensaje) {
        if (mensaje == null) {
            return Utils.construirPlaceholderTemporal(null);
        }
        if (mensaje.getPlaceholderTexto() != null && !mensaje.getPlaceholderTexto().isBlank()) {
            return mensaje.getPlaceholderTexto();
        }
        return Utils.construirPlaceholderTemporal(mensaje.getMensajeTemporalSegundos());
    }

    private void applyLastMessageMeta(ChatIndividualDTO dto, MensajeEntity last) {
        if (dto == null || last == null || !isVisibleMessageForUser(last)) {
            if (dto != null) {
                dto.setUltimaMensajeId(null);
                dto.setUltimaMensajeTipo(null);
                dto.setUltimaMensajeEmisorId(null);
                dto.setUltimaMensajeRaw(null);
                dto.setUltimaMensajeImageUrl(null);
                dto.setUltimaMensajeImageMime(null);
                dto.setUltimaMensajeImageNombre(null);
                dto.setUltimaMensajeAudioUrl(null);
                dto.setUltimaMensajeAudioMime(null);
                dto.setUltimaMensajeAudioDuracionMs(null);
                dto.setUltimaMensajeFileUrl(null);
                dto.setUltimaMensajeFileMime(null);
                dto.setUltimaMensajeFileNombre(null);
                dto.setUltimaMensajeFileSizeBytes(null);
            }
            return;
        }
        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        dto.setUltimaMensajeId(last.getId());
        dto.setUltimaMensajeTipo(tipo.name());
        dto.setUltimaMensajeEmisorId(last.getEmisor() == null ? null : last.getEmisor().getId());
        dto.setUltimaMensajeRaw(last.getContenido());
        dto.setUltimaFecha(last.getFechaEnvio());

        GroupMediaMeta meta = extractMediaMeta(last);
        if (tipo == MessageType.IMAGE) {
            dto.setUltimaMensajeImageUrl(meta.mediaUrl());
            dto.setUltimaMensajeImageMime(meta.mime());
            dto.setUltimaMensajeImageNombre(meta.fileName());
            dto.setUltimaMensajeAudioUrl(null);
            dto.setUltimaMensajeAudioMime(null);
            dto.setUltimaMensajeAudioDuracionMs(null);
            dto.setUltimaMensajeFileUrl(null);
            dto.setUltimaMensajeFileMime(null);
            dto.setUltimaMensajeFileNombre(null);
            dto.setUltimaMensajeFileSizeBytes(null);
            return;
        }
        if (tipo == MessageType.AUDIO) {
            dto.setUltimaMensajeAudioUrl(meta.mediaUrl());
            dto.setUltimaMensajeAudioMime(meta.mime());
            dto.setUltimaMensajeAudioDuracionMs(meta.durMs());
            dto.setUltimaMensajeImageUrl(null);
            dto.setUltimaMensajeImageMime(null);
            dto.setUltimaMensajeImageNombre(null);
            dto.setUltimaMensajeFileUrl(null);
            dto.setUltimaMensajeFileMime(null);
            dto.setUltimaMensajeFileNombre(null);
            dto.setUltimaMensajeFileSizeBytes(null);
            return;
        }
        if (tipo == MessageType.FILE) {
            dto.setUltimaMensajeFileUrl(meta.mediaUrl());
            dto.setUltimaMensajeFileMime(meta.mime());
            dto.setUltimaMensajeFileNombre(meta.fileName());
            dto.setUltimaMensajeFileSizeBytes(meta.sizeBytes());
            dto.setUltimaMensajeImageUrl(null);
            dto.setUltimaMensajeImageMime(null);
            dto.setUltimaMensajeImageNombre(null);
            dto.setUltimaMensajeAudioUrl(null);
            dto.setUltimaMensajeAudioMime(null);
            dto.setUltimaMensajeAudioDuracionMs(null);
            return;
        }
        dto.setUltimaMensajeImageUrl(null);
        dto.setUltimaMensajeImageMime(null);
        dto.setUltimaMensajeImageNombre(null);
        dto.setUltimaMensajeAudioUrl(null);
        dto.setUltimaMensajeAudioMime(null);
        dto.setUltimaMensajeAudioDuracionMs(null);
        dto.setUltimaMensajeFileUrl(null);
        dto.setUltimaMensajeFileMime(null);
        dto.setUltimaMensajeFileNombre(null);
        dto.setUltimaMensajeFileSizeBytes(null);
    }

    private void applyLastMessageMeta(ChatGrupalDTO dto, MensajeEntity last) {
        if (dto == null || last == null || !isVisibleMessageForUser(last)) {
            if (dto != null) {
                dto.setUltimaMensajeId(null);
                dto.setUltimaMensajeTipo(null);
                dto.setUltimaMensajeEmisorId(null);
                dto.setUltimaMensajeRaw(null);
                dto.setUltimaMensajeImageUrl(null);
                dto.setUltimaMensajeImageMime(null);
                dto.setUltimaMensajeImageNombre(null);
                dto.setUltimaMensajeAudioUrl(null);
                dto.setUltimaMensajeAudioMime(null);
                dto.setUltimaMensajeAudioDuracionMs(null);
                dto.setUltimaMensajeFileUrl(null);
                dto.setUltimaMensajeFileMime(null);
                dto.setUltimaMensajeFileNombre(null);
                dto.setUltimaMensajeFileSizeBytes(null);
            }
            return;
        }
        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        dto.setUltimaMensajeId(last.getId());
        dto.setUltimaMensajeTipo(tipo.name());
        dto.setUltimaMensajeEmisorId(last.getEmisor() == null ? null : last.getEmisor().getId());
        dto.setUltimaMensajeRaw(last.getContenido());
        dto.setUltimaFecha(last.getFechaEnvio());

        GroupMediaMeta meta = extractMediaMeta(last);
        if (tipo == MessageType.IMAGE) {
            dto.setUltimaMensajeImageUrl(meta.mediaUrl());
            dto.setUltimaMensajeImageMime(meta.mime());
            dto.setUltimaMensajeImageNombre(meta.fileName());
            dto.setUltimaMensajeAudioUrl(null);
            dto.setUltimaMensajeAudioMime(null);
            dto.setUltimaMensajeAudioDuracionMs(null);
            dto.setUltimaMensajeFileUrl(null);
            dto.setUltimaMensajeFileMime(null);
            dto.setUltimaMensajeFileNombre(null);
            dto.setUltimaMensajeFileSizeBytes(null);
            return;
        }
        if (tipo == MessageType.AUDIO) {
            dto.setUltimaMensajeAudioUrl(meta.mediaUrl());
            dto.setUltimaMensajeAudioMime(meta.mime());
            dto.setUltimaMensajeAudioDuracionMs(meta.durMs());
            dto.setUltimaMensajeImageUrl(null);
            dto.setUltimaMensajeImageMime(null);
            dto.setUltimaMensajeImageNombre(null);
            dto.setUltimaMensajeFileUrl(null);
            dto.setUltimaMensajeFileMime(null);
            dto.setUltimaMensajeFileNombre(null);
            dto.setUltimaMensajeFileSizeBytes(null);
            return;
        }
        if (tipo == MessageType.FILE) {
            dto.setUltimaMensajeFileUrl(meta.mediaUrl());
            dto.setUltimaMensajeFileMime(meta.mime());
            dto.setUltimaMensajeFileNombre(meta.fileName());
            dto.setUltimaMensajeFileSizeBytes(meta.sizeBytes());
            dto.setUltimaMensajeImageUrl(null);
            dto.setUltimaMensajeImageMime(null);
            dto.setUltimaMensajeImageNombre(null);
            dto.setUltimaMensajeAudioUrl(null);
            dto.setUltimaMensajeAudioMime(null);
            dto.setUltimaMensajeAudioDuracionMs(null);
            return;
        }
        dto.setUltimaMensajeImageUrl(null);
        dto.setUltimaMensajeImageMime(null);
        dto.setUltimaMensajeImageNombre(null);
        dto.setUltimaMensajeAudioUrl(null);
        dto.setUltimaMensajeAudioMime(null);
        dto.setUltimaMensajeAudioDuracionMs(null);
        dto.setUltimaMensajeFileUrl(null);
        dto.setUltimaMensajeFileMime(null);
        dto.setUltimaMensajeFileNombre(null);
        dto.setUltimaMensajeFileSizeBytes(null);
    }

    private Map<Long, List<MensajeReaccionEntity>> loadReaccionesPorMensaje(List<MensajeEntity> mensajes) {
        if (mensajes == null || mensajes.isEmpty()) {
            return Map.of();
        }
        List<Long> messageIds = mensajes.stream()
                .map(MensajeEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        List<MensajeReaccionEntity> rows = mensajeReaccionRepository
                .findByMensajeIdInOrderByMensajeIdAscUpdatedAtDescIdDesc(messageIds);
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getMensaje() != null && r.getMensaje().getId() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getMensaje().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private void applyReacciones(MensajeDTO dto, List<MensajeReaccionEntity> reaccionesMensaje) {
        if (dto == null) {
            return;
        }
        if ("EXPIRADO".equalsIgnoreCase(dto.getEstadoTemporal())) {
            dto.setReaccionEmoji(null);
            dto.setReaccionUsuarioId(null);
            dto.setReaccionFecha(null);
            dto.setReacciones(List.of());
            return;
        }
        if (reaccionesMensaje == null || reaccionesMensaje.isEmpty()) {
            dto.setReaccionEmoji(null);
            dto.setReaccionUsuarioId(null);
            dto.setReaccionFecha(null);
            dto.setReacciones(List.of());
            return;
        }
        MensajeReaccionEntity latest = reaccionesMensaje.get(0);
        dto.setReaccionEmoji(latest.getEmoji());
        dto.setReaccionUsuarioId(latest.getUsuario() == null ? null : latest.getUsuario().getId());
        dto.setReaccionFecha(latest.getUpdatedAt() != null ? latest.getUpdatedAt() : latest.getCreatedAt());
        List<MensajeReaccionResumenDTO> resumenes = reaccionesMensaje.stream()
                .map(reaccion -> {
                    MensajeReaccionResumenDTO resumen = new MensajeReaccionResumenDTO();
                    resumen.setUserId(reaccion.getUsuario() == null ? null : reaccion.getUsuario().getId());
                    resumen.setEmoji(reaccion.getEmoji());
                    resumen.setCreatedAt(reaccion.getUpdatedAt() != null ? reaccion.getUpdatedAt() : reaccion.getCreatedAt());
                    return resumen;
                })
                .collect(Collectors.toList());
        dto.setReacciones(resumenes);
    }

    @Override
    public List<MensajeDTO> listarMensajesPorChatId(Long chatId, Integer page, Integer size) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatEntity chat = validateUserPinnedChatAccess(chatId, requesterId);
        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());
        boolean groupChat = chat instanceof ChatGrupalEntity;
        Long cutoff = chatUserStateService.resolveCutoff(chatId, requesterId);
        List<MensajeEntity> mensajes = fetchMessagesPageChronological(chatId, cutoff, page, size);
        marcarAdminTemporalesComoLeidosAlAbrirChat(mensajes, requesterId);
        Map<Long, List<MensajeReaccionEntity>> reaccionesPorMensaje = loadReaccionesPorMensaje(mensajes);

        List<MensajeDTO> salida = mensajes.stream()
                .map(e -> {
                    try {
                        MensajeDTO dto = MappingUtils.mensajeEntityADto(e);
                        applyReacciones(dto, reaccionesPorMensaje.get(e.getId()));
                        if (groupChat) {
                            E2EDiagnosticUtils.ContentDiagnostic historyDiag = E2EDiagnosticUtils.analyze(
                                    dto.getContenido(),
                                    dto.getTipo());
                            boolean senderKeyPresentAtRead = e.getEmisor() != null
                                    && e.getEmisor().getPublicKey() != null
                                    && !e.getEmisor().getPublicKey().isBlank();
                            LOGGER.info(
                                    Constantes.LOG_E2E_HISTORY_OUT_CHAT_ID,
                                    Instant.now(),
                                    traceId,
                                    chatId,
                                    dto.getId(),
                                    dto.getEmisorId(),
                                    dto.getTipo(),
                                    historyDiag.getClassification(),
                                    historyDiag.getLength(),
                                    historyDiag.getHash12(),
                                    historyDiag.hasIv(),
                                    historyDiag.hasCiphertext(),
                                    historyDiag.hasForEmisor(),
                                    historyDiag.hasForReceptores(),
                                    historyDiag.hasForAdmin(),
                                    historyDiag.getForReceptoresKeys(),
                                    senderKeyPresentAtRead);
                            if ("INVALID_JSON".equals(historyDiag.getClassification())) {
                                LOGGER.warn(Constantes.LOG_E2E_HISTORY_OUT_PARSE_WARN,
                                        Instant.now(), traceId, chatId, dto.getId(), historyDiag.getParseErrorClass());
                            }
                        }
                        return dto;
                    } catch (RuntimeException ex) {
                        if (groupChat) {
                            LOGGER.error(Constantes.LOG_E2E_HISTORY_OUT_ERROR,
                                    Instant.now(), traceId, chatId, e.getId(), ex.getClass().getSimpleName());
                        }
                        throw ex;
                    }
                })
                .collect(Collectors.toList());
        encuestaService.enriquecerMensajesConEncuesta(mensajes, salida, requesterId, true);
        return salida;
    }

    @Override
    public List<MensajeDTO> listarMensajesPorChatGrupal(Long chatId, Integer page, Integer size) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatEntity validatedChat = validateUserPinnedChatAccess(chatId, requesterId);
        if (!(validatedChat instanceof ChatGrupalEntity chat)) {
            throw new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId);
        }
        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());

        // Si tienes un repo con este mÃ©todo, Ãºsalo; si no, ordenamos en memoria:
        // List<MensajeEntity> mensajes =
        // mensajeRepository.findByChatIdOrderByFechaEnvioAsc(chatId);
        Long cutoff = chatUserStateService.resolveCutoff(chatId, requesterId);
        List<MensajeEntity> mensajes = fetchMessagesPageChronological(chatId, cutoff, page, size);
        Map<Long, List<MensajeReaccionEntity>> reaccionesPorMensaje = loadReaccionesPorMensaje(mensajes);
        Map<Long, String> currentMemberKeyFp = new LinkedHashMap<>();
        if (chat.getUsuarios() != null) {
            chat.getUsuarios().forEach(u -> {
                if (u != null && u.getId() != null) {
                    currentMemberKeyFp.put(u.getId(), E2EDiagnosticUtils.fingerprint12(u.getPublicKey()));
                }
            });
        }

        List<MensajeDTO> salida = mensajes.stream()
                .map(e -> {
                    try {
                        MensajeDTO dto = MappingUtils.mensajeEntityADto(e);
                        applyReacciones(dto, reaccionesPorMensaje.get(e.getId()));
                        UsuarioEntity emisor = e.getEmisor();
                        if (emisor != null) {
                            dto.setEmisorNombre(emisor.getNombre());
                            dto.setEmisorApellido(emisor.getApellido());
                            if (emisor.getFotoUrl() != null) {
                                dto.setEmisorFoto(Utils.toDataUrlFromUrl(emisor.getFotoUrl(), uploadsRoot));
                            }
                        }
                        E2EDiagnosticUtils.ContentDiagnostic historyDiag = E2EDiagnosticUtils.analyze(
                                dto.getContenido(),
                                dto.getTipo());
                        boolean senderKeyPresentAtRead = e.getEmisor() != null
                                && e.getEmisor().getPublicKey() != null
                                && !e.getEmisor().getPublicKey().isBlank();
                        List<Long> recipientIdsUsed = currentMemberKeyFp.keySet().stream()
                                .filter(id -> !Objects.equals(id, dto.getEmisorId()))
                                .collect(Collectors.toList());
                        Set<String> payloadForReceptoresKeys = E2EDiagnosticUtils.extractForReceptoresKeys(dto.getContenido());
                        String senderCurrentKeyFp = currentMemberKeyFp.getOrDefault(dto.getEmisorId(), "-");
                        LOGGER.info(
                                Constantes.LOG_E2E_HISTORY_OUT_CHAT_GRUPAL,
                                Instant.now(),
                                traceId,
                                chatId,
                                dto.getId(),
                                dto.getEmisorId(),
                                dto.getTipo(),
                                historyDiag.getClassification(),
                                historyDiag.getLength(),
                                historyDiag.getHash12(),
                                historyDiag.hasIv(),
                                historyDiag.hasCiphertext(),
                                historyDiag.hasForEmisor(),
                                historyDiag.hasForReceptores(),
                                historyDiag.hasForAdmin(),
                                historyDiag.getForReceptoresKeys(),
                                recipientIdsUsed,
                                payloadForReceptoresKeys,
                                senderCurrentKeyFp,
                                currentMemberKeyFp,
                                senderKeyPresentAtRead);
                        if ("INVALID_JSON".equals(historyDiag.getClassification())) {
                            LOGGER.warn(Constantes.LOG_E2E_HISTORY_OUT_PARSE_WARN,
                                    Instant.now(), traceId, chatId, dto.getId(), historyDiag.getParseErrorClass());
                        }
                        return dto;
                    } catch (RuntimeException ex) {
                        LOGGER.error(Constantes.LOG_E2E_HISTORY_OUT_ERROR,
                                Instant.now(), traceId, chatId, e.getId(), ex.getClass().getSimpleName());
                        throw ex;
                    }
                })
                .collect(Collectors.toList());
        encuestaService.enriquecerMensajesConEncuesta(mensajes, salida, requesterId, true);
        return salida;
    }

    @Override
    @Transactional
    public ChatMensajeBusquedaPageDTO buscarMensajesEnChat(Long chatId, String q, Integer page, Integer size) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));
        validateSearchAccess(chat, requesterId);

        int safePage = page == null ? DEFAULT_SEARCH_PAGE : Math.max(DEFAULT_SEARCH_PAGE, page);
        int requestedSize = size == null ? DEFAULT_SEARCH_SIZE : size;
        int safeSize = Math.max(1, Math.min(MAX_SEARCH_SIZE, requestedSize));

        ChatMensajeBusquedaPageDTO response = new ChatMensajeBusquedaPageDTO();
        response.setPage(safePage);
        response.setSize(safeSize);

        String normalizedQuery = normalizeForSearch(q == null ? "" : q.trim());
        if (normalizedQuery.isBlank()) {
            response.setItems(List.of());
            response.setTotal(0);
            response.setHasMore(false);
            return response;
        }

        Long cutoff = chatUserStateService.resolveCutoff(chatId, requesterId);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("fechaEnvio"), Sort.Order.desc("id")));
        Page<MensajeEntity> resultPage = mensajeRepository.searchByContenidoBusqueda(
                chatId,
                normalizedQuery,
                cutoff,
                pageable);

        List<ChatMensajeBusquedaItemDTO> items = resultPage.getContent().stream()
                .map(mensaje -> {
                    String searchableContent = mensaje.getContenidoBusqueda();
                    MatchWindow window = findFirstMatchWindow(searchableContent, normalizedQuery);
                    String snippet = window == null
                            ? Utils.truncarSafe(searchableContent, 180)
                            : window.snippet();

                    ChatMensajeBusquedaItemDTO item = new ChatMensajeBusquedaItemDTO();
                    item.setId(mensaje.getId());
                    item.setChatId(mensaje.getChat() == null ? null : mensaje.getChat().getId());
                    item.setEmisorId(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId());
                    item.setEmisorNombre(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getNombre());
                    item.setEmisorApellido(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getApellido());
                    item.setFechaEnvio(mensaje.getFechaEnvio());
                    item.setSnippet(snippet);
                    item.setContenido(snippet);
                    if (window != null) {
                        item.setMatchStart(window.matchStart());
                        item.setMatchEnd(window.matchEnd());
                    }
                    return item;
                })
                .collect(Collectors.toList());

        response.setItems(items);
        response.setTotal(resultPage.getTotalElements());
        response.setHasMore(resultPage.hasNext());
        return response;
    }

    @Override
    @Transactional
    public ChatClearResponseDTO clearChat(Long chatId) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatEntity chat = validateUserPinnedChatAccess(chatId, requesterId);
        UsuarioEntity user = usuarioRepo.findById(requesterId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));

        Long cutoff = mensajeRepository.findMaxIdByChatId(chat.getId()).orElse(null);
        LocalDateTime clearedAt = LocalDateTime.now(ZoneOffset.UTC);
        ChatUserStateEntity state = chatUserStateService.upsertClearState(chat, user, cutoff, clearedAt);

        ChatClearResponseDTO response = new ChatClearResponseDTO();
        response.setOk(true);
        response.setChatId(chat.getId());
        response.setUserId(requesterId);
        response.setClearedBeforeMessageId(state.getClearedBeforeMessageId());
        response.setClearedAt(state.getClearedAt());
        return response;
    }

    @Override
    @Transactional
    public ChatMuteStateDTO muteChat(Long chatId, ChatMuteRequestDTO request) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatEntity chat = validateUserPinnedChatAccess(chatId, requesterId);
        UsuarioEntity user = usuarioRepo.findById(requesterId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));

        MuteConfig config = validateMuteRequestOrThrow(request);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime mutedUntil = config.mutedForever() ? null : now.plusSeconds(config.durationSeconds());
        ChatUserStateEntity state = chatUserStateService.upsertMuteState(chat, user, config.mutedForever(), mutedUntil, now);
        return toMuteStateDto(state, now);
    }

    @Override
    @Transactional
    public ChatMuteStateDTO unmuteChat(Long chatId) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatEntity chat = validateUserPinnedChatAccess(chatId, requesterId);
        UsuarioEntity user = usuarioRepo.findById(requesterId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ChatUserStateEntity state = chatUserStateService.clearMuteState(chat, user, now);
        return toMuteStateDto(state, now);
    }

    @Override
    public List<ChatMuteStateDTO> listarChatsMuteadosActivos() {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return chatUserStateService.findActiveMutesByUser(requesterId, now).stream()
                .map(state -> toMuteStateDto(state, now))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserPinnedChatResponseDTO setPinnedChat(UserPinnedChatRequestDTO request) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        Long chatId = request == null ? null : request.getChatId();

        if (chatId == null) {
            userPinnedChatRepository.deleteById(requesterId);
            return new UserPinnedChatResponseDTO(null, null);
        }

        validateUserPinnedChatAccess(chatId, requesterId);

        UserPinnedChatEntity pinned = userPinnedChatRepository.findById(requesterId)
                .orElseGet(UserPinnedChatEntity::new);
        LocalDateTime now = LocalDateTime.now();
        pinned.setUserId(requesterId);
        pinned.setChatId(chatId);
        pinned.setPinnedAt(now);

        UserPinnedChatEntity saved = userPinnedChatRepository.save(pinned);
        return new UserPinnedChatResponseDTO(saved.getChatId(), saved.getPinnedAt());
    }

    @Override
    @Transactional
    public UserPinnedChatResponseDTO getPinnedChat() {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        UserPinnedChatEntity pinned = userPinnedChatRepository.findById(requesterId).orElse(null);
        if (pinned == null) {
            return new UserPinnedChatResponseDTO(null, null);
        }

        try {
            validateUserPinnedChatAccess(pinned.getChatId(), requesterId);
        } catch (AccessDeniedException | RecursoNoEncontradoException ex) {
            userPinnedChatRepository.deleteById(requesterId);
            return new UserPinnedChatResponseDTO(null, null);
        }
        return new UserPinnedChatResponseDTO(pinned.getChatId(), pinned.getPinnedAt());
    }

    @Override
    @Transactional
    public ChatPinnedMessageDTO getPinnedMessage(Long chatId) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        validatePinnedChatAccess(chatId, requesterId);

        LocalDateTime now = LocalDateTime.now();
        ChatPinnedMessageEntity pin = chatPinnedMessageRepository
                .findByChatIdAndActivoTrueAndUnpinnedAtIsNullAndExpiresAtAfter(chatId, now)
                .orElse(null);
        if (pin == null) {
            return null;
        }
        MensajeEntity mensaje = findActiveMessageForPin(chatId, pin.getMessageId(), now);
        if (mensaje == null) {
            pin.setActivo(false);
            pin.setUnpinnedAt(now);
            pin.setUpdatedAt(now);
            chatPinnedMessageRepository.save(pin);
            return null;
        }
        Long cutoff = chatUserStateService.resolveCutoff(chatId, requesterId);
        if (cutoff != null && mensaje.getId() != null && mensaje.getId() <= cutoff) {
            return null;
        }

        return chatPinnedMessageMapper.toDto(chatId, pin, mensaje);
    }

    @Override
    @Transactional
    public ChatPinnedMessageDTO pinMessage(Long chatId, ChatPinMessageRequestDTO request) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        validatePinnedChatAccess(chatId, requesterId);

        if (request == null || request.getMessageId() == null) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, "messageId es obligatorio");
        }

        long durationSeconds = validatePinDurationOrThrow(request.getDurationSeconds());
        LocalDateTime now = LocalDateTime.now();
        MensajeEntity mensaje = findActiveMessageForPin(chatId, request.getMessageId(), now);
        if (mensaje == null) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_CHAT_PIN_MESSAGE_NOT_IN_CHAT,
                    "El mensaje no pertenece al chat o no esta activo");
        }
        Long cutoff = chatUserStateService.resolveCutoff(chatId, requesterId);
        if (cutoff != null && mensaje.getId() != null && mensaje.getId() <= cutoff) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_CHAT_PIN_MESSAGE_NOT_IN_CHAT,
                    "El mensaje no pertenece al chat o no esta activo");
        }

        ChatPinnedMessageEntity pin = chatPinnedMessageRepository.findByChatId(chatId)
                .orElseGet(ChatPinnedMessageEntity::new);

        pin.setChatId(chatId);
        pin.setMessageId(mensaje.getId());
        Long senderId = mensaje.getEmisor() == null ? requesterId : mensaje.getEmisor().getId();
        pin.setSenderId(senderId);
        pin.setPinnedByUserId(requesterId);
        pin.setPinnedAt(now);
        pin.setExpiresAt(now.plusSeconds(durationSeconds));
        pin.setUnpinnedAt(null);
        pin.setActivo(true);
        if (pin.getCreatedAt() == null) {
            pin.setCreatedAt(now);
        }
        pin.setUpdatedAt(now);

        ChatPinnedMessageEntity saved = chatPinnedMessageRepository.save(pin);
        return chatPinnedMessageMapper.toDto(chatId, saved, mensaje);
    }

    @Override
    @Transactional
    public void unpinMessage(Long chatId) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        validatePinnedChatAccess(chatId, requesterId);

        LocalDateTime now = LocalDateTime.now();
        ChatPinnedMessageEntity pin = findActivePinOrThrow(chatId, now);
        pin.setActivo(false);
        pin.setUnpinnedAt(now);
        pin.setUpdatedAt(now);
        chatPinnedMessageRepository.save(pin);
    }

    @Override
    public GroupMediaPageDTO listarMediaPorChatGrupal(Long chatId, String cursor, Integer size, String types) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = chatGrupalRepo.findByIdWithUsuarios(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
        if (!chat.isActivo()) {
            throw new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId);
        }

        boolean requesterIsActiveMember = chat.getUsuarios() != null
                && chat.getUsuarios().stream()
                .anyMatch(u -> u != null && Objects.equals(u.getId(), requesterId) && u.isActivo());
        if (!requesterIsActiveMember) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        int safeSize = size == null ? DEFAULT_MEDIA_SIZE : Math.max(1, Math.min(MAX_MEDIA_SIZE, size));
        List<MessageType> mediaTypes = parseMediaTypes(types);
        CursorPosition cursorPosition = parseCursor(cursor, chatId);
        Long cutoff = chatUserStateService.resolveCutoff(chatId, requesterId);

        Pageable pageable = PageRequest.of(0, safeSize + 1);
        List<MensajeEntity> fetched = mensajeRepository.findGroupMediaPage(
                chatId,
                mediaTypes,
                cutoff,
                cursorPosition.fechaEnvio(),
                cursorPosition.messageId(),
                pageable);

        boolean hasMore = fetched.size() > safeSize;
        List<MensajeEntity> visible = hasMore ? fetched.subList(0, safeSize) : fetched;
        List<GroupMediaItemDTO> items = visible.stream()
                .map(mensaje -> groupMediaItemMapper.toDto(
                        mensaje,
                        buildNombreCompleto(
                                mensaje.getEmisor() == null ? null : mensaje.getEmisor().getNombre(),
                                mensaje.getEmisor() == null ? null : mensaje.getEmisor().getApellido()),
                        extractMediaMeta(mensaje)))
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasMore && !visible.isEmpty()) {
            MensajeEntity last = visible.get(visible.size() - 1);
            nextCursor = encodeCursor(last.getFechaEnvio(), last.getId());
        }

        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId())
                .orElse(E2EDiagnosticUtils.newTraceId());
        LOGGER.info(
                Constantes.LOG_E2E_GROUP_MEDIA_LIST,
                traceId,
                chatId,
                requesterId,
                cursor,
                safeSize,
                mediaTypes.stream().map(Enum::name).collect(Collectors.joining(",")),
                items.size(),
                hasMore);
        for (GroupMediaItemDTO item : items) {
            E2EDiagnosticUtils.ContentDiagnostic diag = E2EDiagnosticUtils.analyze(item.getContenidoRaw(), item.getTipo());
            LOGGER.info(
                    Constantes.LOG_E2E_GROUP_MEDIA_ITEM,
                    traceId,
                    chatId,
                    item.getMessageId(),
                    item.getTipo(),
                    diag.getClassification(),
                    diag.getLength(),
                    diag.getHash12());
        }

        GroupMediaPageDTO out = new GroupMediaPageDTO();
        out.setItems(items);
        out.setHasMore(hasMore);
        out.setNextCursor(nextCursor);
        return out;
    }

    @Override
    public List<MensajeDTO> listarMensajesPorChatIdAdmin(Long chatId, Boolean includeExpired) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        boolean requesterPuedeAuditar = usuarioRepo.findById(requesterId)
                .map(this::esAdminOAuditor)
                .orElse(false);
        if (!requesterPuedeAuditar) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }

        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));

        boolean incluirExpirados = Boolean.TRUE.equals(includeExpired);
        List<MensajeEntity> mensajes = incluirExpirados
                ? mensajeRepository.findByChatIdOrderByFechaEnvioAscIncluyendoExpirados(chat.getId())
                : mensajeRepository.findByChatIdOrderByFechaEnvioAsc(chat.getId());

        Map<Long, MensajeTemporalAuditoriaEntity> auditoriaPorMensaje = cargarAuditoriaPorMensajes(mensajes);
        boolean mostrarOriginalAuditoria = incluirExpirados && requesterPuedeAuditar;
        Map<Long, List<MensajeReaccionEntity>> reaccionesPorMensaje = loadReaccionesPorMensaje(mensajes);
        List<MensajeDTO> salida = mensajes.stream()
                .map(e -> {
                    MensajeDTO dto = MappingUtils.mensajeEntityADto(e);
                    applyReacciones(dto, reaccionesPorMensaje.get(e.getId()));
                    aplicarCamposAuditoriaAdmin(dto, e, auditoriaPorMensaje.get(e.getId()), mostrarOriginalAuditoria, requesterId);
                    LOGGER.info(Constantes.LOG_E2E_ADMIN_CHAT_MESSAGES_RAW,
                            chatId,
                            e.getId(),
                            E2EDiagnosticUtils.analyze(
                                    e.getContenido(),
                                    e.getTipo() == null ? null : e.getTipo().name()).getClassification(),
                            false,
                            false);
                    return dto;
                })
                .collect(Collectors.toList());
        encuestaService.enriquecerMensajesConEncuesta(mensajes, salida, requesterId, true);
        return salida;
    }

    @Override
    public List<ChatResumenDTO> listarConversacionesDeUsuario(Long usuarioId, Boolean includeExpired) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        boolean requesterPuedeAuditar = usuarioRepo.findById(requesterId)
                .map(this::esAdminOAuditor)
                .orElse(false);
        if (!requesterPuedeAuditar) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }

        List<ChatIndividualEntity> individuales = chatIndRepo.findAllByUsuario1IdOrUsuario2Id(usuarioId, usuarioId);
        List<ChatGrupalEntity> grupales = chatGrupalRepo.findAllByUsuariosId(usuarioId);
        List<Long> chatIds = new ArrayList<>();
        individuales.forEach(ci -> chatIds.add(ci.getId()));
        grupales.forEach(cg -> chatIds.add(cg.getId()));

        Map<Long, Integer> totalMensajesPorChat = new HashMap<>();
        Map<Long, MensajeEntity> ultimoMensajePorChat = new HashMap<>();
        boolean incluirExpirados = Boolean.TRUE.equals(includeExpired);
        if (!chatIds.isEmpty()) {
            (incluirExpirados
                    ? mensajeRepository.countActivosByChatIdsIncluyendoExpirados(chatIds)
                    : mensajeRepository.countActivosByChatIds(chatIds))
                    .forEach(row -> totalMensajesPorChat.put((Long) row[0], ((Long) row[1]).intValue()));
            (incluirExpirados
                    ? mensajeRepository.findLatestByChatIdsIncluyendoExpirados(chatIds)
                    : mensajeRepository.findLatestByChatIds(chatIds))
                    .forEach(m -> ultimoMensajePorChat.put(m.getChat().getId(), m));
        }

        Map<Long, MensajeTemporalAuditoriaEntity> auditoriaUltimosMensajes = cargarAuditoriaPorMensajes(
                new ArrayList<>(ultimoMensajePorChat.values()));
        boolean mostrarOriginalAuditoria = incluirExpirados && requesterPuedeAuditar;

        List<ChatResumenDTO> resumenes = new ArrayList<>();
        for (ChatIndividualEntity ci : individuales) {
            if (ci.isAdminDirect() && totalMensajesPorChat.getOrDefault(ci.getId(), 0) == 0) {
                continue;
            }
            ChatResumenDTO dto = new ChatResumenDTO();
            dto.setId(ci.getId());
            dto.setTipo(Constantes.CHAT_TIPO_INDIVIDUAL);
            dto.setNombreChat(ci.getUsuario1().getNombre() + Constantes.MSG_Y + ci.getUsuario2().getNombre());
            dto.setAdminDirect(ci.isAdminDirect());
            dto.setTotalMensajes(totalMensajesPorChat.getOrDefault(ci.getId(), 0));
            MensajeEntity ultimo = ultimoMensajePorChat.get(ci.getId());
            applyAdminRawMetadata(
                    dto,
                    ultimo,
                    ultimo == null ? null : auditoriaUltimosMensajes.get(ultimo.getId()),
                    mostrarOriginalAuditoria,
                    requesterId);
            resumenes.add(dto);
        }

        for (ChatGrupalEntity cg : grupales) {
            ChatResumenDTO dto = new ChatResumenDTO();
            dto.setId(cg.getId());
            dto.setTipo(Constantes.CHAT_TIPO_GRUPAL);
            dto.setNombreChat(cg.getNombreGrupo() + Constantes.MSG_GRUPO_SUFFIX);
            dto.setChatCerrado(cg.isClosed());
            dto.setClosed(cg.isClosed());
            dto.setChatCerradoMotivo(cg.isClosed() ? cg.getClosedReason() : null);
            dto.setReason(cg.isClosed() ? cg.getClosedReason() : null);
            dto.setTotalMensajes(totalMensajesPorChat.getOrDefault(cg.getId(), 0));
            MensajeEntity ultimo = ultimoMensajePorChat.get(cg.getId());
            applyAdminRawMetadata(
                    dto,
                    ultimo,
                    ultimo == null ? null : auditoriaUltimosMensajes.get(ultimo.getId()),
                    mostrarOriginalAuditoria,
                    requesterId);
            resumenes.add(dto);
        }

        return resumenes;
    }

    private long normalizeAdminDirectExpiry(Long requestedSeconds) {
        if (requestedSeconds == null || requestedSeconds <= 0) {
            return DEFAULT_ADMIN_DIRECT_EXPIRES_AFTER_READ_SECONDS;
        }
        return requestedSeconds;
    }

    private ChatIndividualEntity resolveOrCreateAdminDirectChat(UsuarioEntity admin,
                                                                UsuarioEntity receptor,
                                                                Long requestedChatId) {
        if (admin == null || admin.getId() == null || receptor == null || receptor.getId() == null) {
            throw new IllegalArgumentException("participantes invalidos para chat admin directo");
        }

        if (requestedChatId != null) {
            ChatIndividualEntity chat = chatIndRepo.findById(requestedChatId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_INDIVIDUAL_NO_ENCONTRADO));
            boolean samePair = (Objects.equals(chat.getUsuario1().getId(), admin.getId())
                    && Objects.equals(chat.getUsuario2().getId(), receptor.getId()))
                    || (Objects.equals(chat.getUsuario1().getId(), receptor.getId())
                    && Objects.equals(chat.getUsuario2().getId(), admin.getId()));
            if (!chat.isAdminDirect() || !samePair) {
                throw new IllegalArgumentException("chatId no pertenece a chat admin directo del par solicitado");
            }
            return chat;
        }

        return chatIndRepo.findAdminDirectChatBetween(admin.getId(), receptor.getId())
                .orElseGet(() -> {
                    ChatIndividualEntity chat = new ChatIndividualEntity();
                    chat.setUsuario1(admin);
                    chat.setUsuario2(receptor);
                    chat.setAdminDirect(true);
                    return chatIndRepo.save(chat);
                });
    }

    private MensajeEntity persistAdminDirectMessage(UsuarioEntity admin,
                                                    UsuarioEntity receptor,
                                                    ChatIndividualEntity chat,
                                                    String contenido,
                                                    long expiresAfterReadSeconds) {
        LocalDateTime ahora = LocalDateTime.now();
        MensajeEntity mensaje = new MensajeEntity();
        mensaje.setChat(chat);
        mensaje.setEmisor(admin);
        mensaje.setReceptor(receptor);
        mensaje.setTipo(MessageType.TEXT);
        mensaje.setContenido(contenido);
        mensaje.setContenidoBusqueda(normalizeForSearch(contenido));
        mensaje.setFechaEnvio(ahora);
        mensaje.setActivo(true);
        mensaje.setLeido(false);
        mensaje.setReenviado(false);
        mensaje.setEditado(false);
        mensaje.setMensajeTemporal(true);
        mensaje.setMensajeTemporalSegundos(expiresAfterReadSeconds);
        mensaje.setExpiraEn(null);
        mensaje.setAdminMessage(true);
        mensaje.setExpiresAfterReadSeconds(expiresAfterReadSeconds);
        mensaje.setFirstReadAt(null);
        mensaje.setExpireAt(null);
        mensaje.setExpiredByPolicy(false);
        return mensajeRepository.save(mensaje);
    }

    @Override
    public GroupDetailDTO obtenerDetalleGrupo(Long groupId) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = findActiveGroupOrThrow(groupId);

        if (!isActiveGroupMember(chat, authenticatedUserId)) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        if (ensureCreatorAdmin(chat)) {
            chat = chatGrupalRepo.save(chat);
        }

        GroupDetailDTO dto = new GroupDetailDTO();
        dto.setId(chat.getId());
        dto.setNombreGrupo(chat.getNombreGrupo());
        dto.setFotoGrupo(chat.getFotoUrl());
        dto.setDescripcion(chat.getDescripcion());
        dto.setVisibilidad(chat.getVisibilidad());
        dto.setFechaCreacion(chat.getFechaCreacion());
        dto.setMediaCount(chat.getMediaCount());
        dto.setFilesCount(chat.getFilesCount());
        dto.setCanEditGroup(canEditGroupMetadata(chat, authenticatedUserId));

        UsuarioEntity creador = chat.getCreador();
        if (creador != null) {
            dto.setIdCreador(creador.getId());
            String nombreCreador = ((creador.getNombre() == null ? "" : creador.getNombre())
                    + (creador.getApellido() == null || creador.getApellido().isBlank() ? "" : " " + creador.getApellido()))
                    .trim();
            dto.setNombreCreador(nombreCreador.isEmpty() ? creador.getNombre() : nombreCreador);
        } else {
            dto.setIdCreador(null);
            dto.setNombreCreador(null);
        }

        final ChatGrupalEntity chatFinal = chat;
        List<GroupMemberDTO> miembros = (chatFinal.getUsuarios() == null ? List.<UsuarioEntity>of() : chatFinal.getUsuarios())
                .stream()
                .map(u -> {
                    GroupMemberDTO m = new GroupMemberDTO();
                    m.setId(u.getId());
                    m.setNombre(u.getNombre());
                    m.setApellido(u.getApellido());
                    String foto = u.getFotoUrl();
                    if (foto != null && foto.startsWith(Constantes.UPLOADS_PREFIX)) {
                        String dataUrl = Utils.toDataUrlFromUrl(foto, uploadsRoot);
                        m.setFoto(dataUrl != null ? dataUrl : foto);
                    } else {
                        m.setFoto(foto);
                    }
                    boolean esAdmin = chatFinal.getAdmins() != null
                            && chatFinal.getAdmins().stream().anyMatch(a -> Objects.equals(a.getId(), u.getId()));
                    if (!esAdmin && chatFinal.getCreador() != null && Objects.equals(chatFinal.getCreador().getId(), u.getId())) {
                        esAdmin = true;
                    }
                    m.setRolGrupo(esAdmin ? GroupRole.ADMIN : GroupRole.MIEMBRO);
                    if (estadoUsuarioManager != null) {
                        boolean conectado = estadoUsuarioManager.estaConectado(u.getId());
                        m.setEstado(conectado ? Constantes.ESTADO_CONECTADO : Constantes.ESTADO_DESCONECTADO);
                    }
                    return m;
                })
                .collect(Collectors.toList());

        dto.setMiembros(miembros);
        List<Long> memberIdsDetail = miembros.stream()
                .map(GroupMemberDTO::getId)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        Map<Long, String> memberKeyFp = new LinkedHashMap<>();
        if (chatFinal.getUsuarios() != null) {
            chatFinal.getUsuarios().forEach(u -> {
                if (u != null && u.getId() != null) {
                    memberKeyFp.put(u.getId(), E2EDiagnosticUtils.fingerprint12(u.getPublicKey()));
                }
            });
        }
        LOGGER.info(Constantes.LOG_E2E_GROUP_DETAIL_MEMBERS,
                Instant.now(), groupId, authenticatedUserId, memberIdsDetail, memberIdsDetail.size(), memberKeyFp);
        return dto;
    }

    @Override
    @Transactional
    public GroupDetailDTO actualizarMetadataGrupo(Long groupId, GroupMetadataUpdateDTO dto) {
        if (groupId == null) {
            throw new IllegalArgumentException(Constantes.MSG_GROUP_ID_OBLIGATORIO);
        }
        if (dto == null) {
            throw new IllegalArgumentException("payload de actualizacion vacio");
        }

        boolean wantsUpdateNombre = dto.getNombreGrupo() != null;
        boolean wantsUpdateDescripcion = dto.getDescripcion() != null;
        boolean wantsUpdateFoto = dto.getFotoGrupo() != null;
        if (!wantsUpdateNombre && !wantsUpdateDescripcion && !wantsUpdateFoto) {
            throw new IllegalArgumentException("Debes enviar al menos uno: nombreGrupo, descripcion o fotoGrupo");
        }

        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = findActiveGroupOrThrow(groupId);

        if (!isActiveGroupMember(chat, requesterId)) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }
        if (!canEditGroupMetadata(chat, requesterId)) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }
        ensureCreatorAdmin(chat);

        if (wantsUpdateNombre) {
            String nombre = dto.getNombreGrupo() == null ? null : dto.getNombreGrupo().trim();
            if (nombre == null || nombre.isBlank()) {
                throw new IllegalArgumentException("nombreGrupo no puede estar vacio");
            }
            if (nombre.length() > MAX_GROUP_NAME_LENGTH) {
                throw new IllegalArgumentException("nombreGrupo supera el maximo de " + MAX_GROUP_NAME_LENGTH + " caracteres");
            }
            chat.setNombreGrupo(nombre);
        }

        if (wantsUpdateDescripcion) {
            String descripcion = dto.getDescripcion() == null ? null : dto.getDescripcion().trim();
            if (descripcion != null && descripcion.length() > MAX_GROUP_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException("descripcion supera el maximo de " + MAX_GROUP_DESCRIPTION_LENGTH + " caracteres");
            }
            chat.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion);
        }

        if (wantsUpdateFoto) {
            String foto = dto.getFotoGrupo() == null ? "" : dto.getFotoGrupo().trim();
            if (foto.isBlank()) {
                chat.setFotoUrl(null);
            } else if (foto.startsWith(Constantes.DATA_IMAGE_PREFIX)) {
                chat.setFotoUrl(Utils.saveDataUrlToUploads(foto, Constantes.DIR_GROUP_PHOTOS, uploadsRoot, uploadsBaseUrl));
            } else if (Utils.isPublicUrl(foto)) {
                chat.setFotoUrl(foto);
            } else {
                throw new IllegalArgumentException("fotoGrupo debe ser data:image/* o URL publica");
            }
        }

        chatGrupalRepo.save(chat);
        return obtenerDetalleGrupo(chat.getId());
    }

    @Override
    @Transactional
    public void setAdminGrupo(Long groupId, Long targetUserId, boolean makeAdmin) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = findActiveGroupOrThrow(groupId);

        if (!isActiveGroupMember(chat, requesterId)) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        ensureCreatorAdmin(chat);

        boolean requesterCanManage = isGroupAdmin(chat, requesterId) || isGroupCreator(chat, requesterId);
        if (!requesterCanManage) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }

        UsuarioEntity target = usuarioRepo.findById(targetUserId).orElseThrow();
        if (!isActiveGroupMember(chat, targetUserId)) {
            throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }
        if (!makeAdmin && isGroupCreator(chat, targetUserId)) {
            throw new AccessDeniedException(Constantes.MSG_NO_SE_PUEDE_QUITAR_ADMIN_FUNDADOR);
        }

        if (makeAdmin) {
            chat.getAdmins().add(target);
        } else {
            chat.getAdmins().removeIf(u -> Objects.equals(u.getId(), targetUserId));
            if ((chat.getAdmins() == null || chat.getAdmins().isEmpty()) && chat.getCreador() != null) {
                chat.getAdmins().add(chat.getCreador());
            }
        }

        chatGrupalRepo.save(chat);
    }

    @Override
    @Transactional
    public GroupMemberExpulsionResponseDTO expulsarMiembroDeGrupo(Long groupId, Long targetUserId) {
        if (groupId == null) {
            throw new IllegalArgumentException(Constantes.MSG_GROUP_ID_OBLIGATORIO);
        }
        if (targetUserId == null) {
            throw new IllegalArgumentException("userId es obligatorio.");
        }

        Long actorId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = findActiveGroupOrThrow(groupId);

        if (!isActiveGroupMember(chat, actorId)) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        ensureCreatorAdmin(chat);

        boolean actorCanManage = isGroupAdmin(chat, actorId) || isGroupCreator(chat, actorId);
        if (!actorCanManage) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }

        if (!isActiveGroupMember(chat, targetUserId)) {
            throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }
        if (Objects.equals(actorId, targetUserId)) {
            throw new IllegalArgumentException(Constantes.MSG_NO_SE_PUEDE_EXPULSAR_A_SI_MISMO);
        }
        if (isGroupCreator(chat, targetUserId)) {
            throw new AccessDeniedException(Constantes.MSG_NO_SE_PUEDE_EXPULSAR_FUNDADOR);
        }

        UsuarioEntity actor = findActiveGroupMemberEntity(chat, actorId)
                .orElseThrow(() -> new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO));
        UsuarioEntity target = findActiveGroupMemberEntity(chat, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO));

        boolean targetWasAdmin = isGroupAdmin(chat, targetUserId);

        chat.getUsuarios().removeIf(u -> u != null && Objects.equals(u.getId(), targetUserId));
        if (chat.getAdmins() != null) {
            chat.getAdmins().removeIf(u -> u != null && Objects.equals(u.getId(), targetUserId));
        }
        chatGrupalRepo.save(chat);

        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());
        LOGGER.info(Constantes.LOG_GROUP_MEMBER_EXPELLED_AUDIT,
                traceId, actorId, targetUserId, groupId, actorCanManage, targetWasAdmin);

        MensajeDTO expelledEvent = crearMensajeSistemaExpulsion(chat, actor, target);
        messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT_GRUPAL + groupId, expelledEvent);
        messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT + targetUserId, expelledEvent);
        messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT + actorId, expelledEvent);

        return new GroupMemberExpulsionResponseDTO(
                true,
                Constantes.MSG_MIEMBRO_EXPULSADO_GRUPO,
                groupId,
                targetUserId,
                actorId,
                buildNombreCompleto(actor.getNombre(), actor.getApellido()));
    }

    private ChatGrupalEntity findActiveGroupOrThrow(Long groupId) {
        ChatGrupalEntity chat = chatGrupalRepo.findById(groupId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + groupId));
        if (!chat.isActivo()) {
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + groupId);
        }
        return chat;
    }

    private boolean isActiveGroupMember(ChatGrupalEntity chat, Long userId) {
        return chat != null
                && userId != null
                && chat.getUsuarios() != null
                && chat.getUsuarios().stream()
                .anyMatch(u -> u != null && Objects.equals(u.getId(), userId) && u.isActivo());
    }

    private Optional<UsuarioEntity> findActiveGroupMemberEntity(ChatGrupalEntity chat, Long userId) {
        if (chat == null || userId == null || chat.getUsuarios() == null) {
            return Optional.empty();
        }
        return chat.getUsuarios().stream()
                .filter(Objects::nonNull)
                .filter(UsuarioEntity::isActivo)
                .filter(u -> Objects.equals(u.getId(), userId))
                .findFirst();
    }

    private boolean isGroupAdmin(ChatGrupalEntity chat, Long userId) {
        return chat != null
                && userId != null
                && chat.getAdmins() != null
                && chat.getAdmins().stream()
                .anyMatch(a -> a != null && Objects.equals(a.getId(), userId));
    }

    private boolean isGroupCreator(ChatGrupalEntity chat, Long userId) {
        return chat != null
                && userId != null
                && chat.getCreador() != null
                && Objects.equals(chat.getCreador().getId(), userId);
    }

    private boolean canEditGroupMetadata(ChatGrupalEntity chat, Long userId) {
        return isActiveGroupMember(chat, userId) && (isGroupAdmin(chat, userId) || isGroupCreator(chat, userId));
    }

    private boolean ensureCreatorAdmin(ChatGrupalEntity chat) {
        if (chat == null || chat.getCreador() == null) {
            return false;
        }
        if (chat.getAdmins() == null) {
            chat.setAdmins(new LinkedHashSet<>());
        }
        Long creatorId = chat.getCreador().getId();
        boolean alreadyAdmin = chat.getAdmins().stream()
                .filter(Objects::nonNull)
                .anyMatch(a -> Objects.equals(a.getId(), creatorId));
        if (alreadyAdmin) {
            return false;
        }
        chat.getAdmins().add(chat.getCreador());
        return true;
    }

    private List<MensajeEntity> fetchMessagesPageChronological(Long chatId, Long cutoff, Integer page, Integer size) {
        Pageable pageable = buildMessagesPageable(page, size);
        List<MensajeEntity> content = new ArrayList<>(mensajeRepository.findByChatIdVisibleAfter(chatId, cutoff, pageable).getContent());
        content.removeIf(m -> !isVisibleMessageForUser(m));
        content.sort(Comparator.comparing(MensajeEntity::getFechaEnvio).thenComparing(MensajeEntity::getId));
        return content;
    }

    private void marcarAdminTemporalesComoLeidosAlAbrirChat(List<MensajeEntity> mensajes, Long requesterId) {
        if (mensajes == null || mensajes.isEmpty() || requesterId == null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<MensajeEntity> pendientes = mensajes.stream()
                .filter(Objects::nonNull)
                .filter(m -> !m.isLeido())
                .filter(m -> m.isActivo())
                .filter(m -> m.getReceptor() != null && Objects.equals(m.getReceptor().getId(), requesterId))
                .collect(Collectors.toList());
        if (pendientes.isEmpty()) {
            return;
        }

        pendientes.forEach(mensaje -> {
            Long chatId = mensaje.getChat() == null ? null : mensaje.getChat().getId();
            boolean adminTemporal = mensaje.isAdminMessage()
                    && (mensaje.isMensajeTemporal()
                    || (mensaje.getExpiresAfterReadSeconds() != null && mensaje.getExpiresAfterReadSeconds() > 0));
            LOGGER.info("[ADMIN-TEMP-READ] start mensajeId={} chatId={} receptorId={} authUserId={} adminMessage={} mensajeTemporal={} leidoAntes={}",
                    mensaje.getId(),
                    chatId,
                    mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                    requesterId,
                    mensaje.isAdminMessage(),
                    mensaje.isMensajeTemporal(),
                    mensaje.isLeido());
            mensaje.setLeido(true);
            if (adminTemporal) {
                iniciarExpiracionAdminSiCorrespondeDesdeChatOpen(mensaje, ahora);
            }
            String estadoTemporal = mensaje.getExpiraEn() == null ? "NO_TEMPORAL"
                    : (mensaje.getExpiraEn().isAfter(LocalDateTime.now()) ? "ACTIVO" : "EXPIRADO");
            LOGGER.info("[ADMIN-TEMP-READ] persisted mensajeId={} leidoDespues=true firstReadAt={} expireAt={} estadoTemporal={}",
                    mensaje.getId(),
                    mensaje.getFirstReadAt(),
                    mensaje.getExpireAt(),
                    estadoTemporal);
        });
        mensajeRepository.saveAll(pendientes);

        pendientes.forEach(mensaje -> {
            Long emisorId = mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId();
            if (emisorId != null) {
                Map<String, Long> payload = new HashMap<>();
                payload.put(Constantes.KEY_MENSAJE_ID, mensaje.getId());
                messagingTemplate.convertAndSend(Constantes.WS_TOPIC_LEIDO + emisorId, payload);
                MensajeDTO dto = MappingUtils.mensajeEntityADto(mensaje);
                messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT + emisorId, dto);
                messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT + requesterId, dto);
            } else {
                LOGGER.info("[ADMIN-TEMP-READ] skip mensajeId={} chatId={} receptorId={} authUserId={} reason=no-emisor-id",
                        mensaje.getId(),
                        mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                        mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                        requesterId);
            }
        });
    }

    private void iniciarExpiracionAdminSiCorrespondeDesdeChatOpen(MensajeEntity mensaje, LocalDateTime ahora) {
        if (mensaje == null || !mensaje.isAdminMessage() || mensaje.getFirstReadAt() != null) {
            return;
        }
        long segundos = mensaje.getExpiresAfterReadSeconds() != null && mensaje.getExpiresAfterReadSeconds() > 0
                ? mensaje.getExpiresAfterReadSeconds()
                : (mensaje.getMensajeTemporalSegundos() != null && mensaje.getMensajeTemporalSegundos() > 0
                ? mensaje.getMensajeTemporalSegundos()
                : DEFAULT_ADMIN_DIRECT_EXPIRES_AFTER_READ_SECONDS);
        LocalDateTime base = ahora != null ? ahora : LocalDateTime.now();
        LocalDateTime expireAt = base.plusSeconds(segundos);
        mensaje.setFirstReadAt(base);
        mensaje.setExpireAt(expireAt);
        mensaje.setExpiraEn(expireAt);
        mensaje.setMensajeTemporal(true);
        mensaje.setMensajeTemporalSegundos(segundos);
    }

    private Pageable buildMessagesPageable(Integer page, Integer size) {
        int safePage = page == null ? DEFAULT_MESSAGES_PAGE : Math.max(DEFAULT_MESSAGES_PAGE, page);
        int requestedSize = size == null ? DEFAULT_MESSAGES_SIZE : size;
        int safeSize = Math.max(1, Math.min(MAX_MESSAGES_SIZE, requestedSize));
        Sort sort = Sort.by(Sort.Order.desc("fechaEnvio"), Sort.Order.desc("id"));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private void validateSearchAccess(ChatEntity chat, Long requesterId) {
        if (chat instanceof ChatIndividualEntity individual) {
            boolean isMember = (individual.getUsuario1() != null && Objects.equals(individual.getUsuario1().getId(), requesterId))
                    || (individual.getUsuario2() != null && Objects.equals(individual.getUsuario2().getId(), requesterId));
            if (!isMember) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            return;
        }

        if (chat instanceof ChatGrupalEntity grupal) {
            if (!grupal.isActivo()) {
                throw new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chat.getId());
            }
            boolean isMember = grupal.getUsuarios() != null
                    && grupal.getUsuarios().stream()
                    .anyMatch(u -> u != null && Objects.equals(u.getId(), requesterId) && u.isActivo());
            if (!isMember) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            return;
        }

        throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
    }

    private ChatPinnedMessageEntity findActivePinOrThrow(Long chatId, LocalDateTime now) {
        return chatPinnedMessageRepository.findByChatIdAndActivoTrueAndUnpinnedAtIsNullAndExpiresAtAfter(chatId, now)
                .orElseThrow(() -> semanticError(
                        HttpStatus.NOT_FOUND,
                        Constantes.ERR_CHAT_PINNED_NOT_FOUND,
                        "No hay mensaje fijado activo"));
    }

    private ChatEntity validateUserPinnedChatAccess(Long chatId, Long requesterId) {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId es obligatorio");
        }

        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));

        if (chat instanceof ChatIndividualEntity individual) {
            boolean isMember = (individual.getUsuario1() != null && Objects.equals(individual.getUsuario1().getId(), requesterId))
                    || (individual.getUsuario2() != null && Objects.equals(individual.getUsuario2().getId(), requesterId));
            if (!isMember) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            return chat;
        }

        if (chat instanceof ChatGrupalEntity) {
            ChatGrupalEntity group = chatGrupalRepo.findByIdWithUsuarios(chatId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
            if (!group.isActivo()) {
                throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId);
            }
            if (!isActiveGroupMember(group, requesterId)) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            return group;
        }

        throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
    }

    private ChatEntity validatePinnedChatAccess(Long chatId, Long requesterId) {
        if (chatId == null) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_RESPUESTA_INVALIDA, "chatId es obligatorio");
        }

        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> semanticError(
                        HttpStatus.NOT_FOUND,
                        Constantes.ERR_NO_ENCONTRADO,
                        Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));

        boolean requesterIsAdmin = securityUtils.hasRole(Constantes.ADMIN) || securityUtils.hasRole(Constantes.ROLE_ADMIN);
        if (chat instanceof ChatIndividualEntity individual) {
            boolean isMember = (individual.getUsuario1() != null && Objects.equals(individual.getUsuario1().getId(), requesterId))
                    || (individual.getUsuario2() != null && Objects.equals(individual.getUsuario2().getId(), requesterId));
            if (!isMember && !requesterIsAdmin) {
                throw semanticError(HttpStatus.FORBIDDEN, Constantes.ERR_CHAT_PIN_FORBIDDEN, "No autorizado para fijar mensajes en este chat");
            }
            return chat;
        }

        if (chat instanceof ChatGrupalEntity) {
            ChatGrupalEntity group = chatGrupalRepo.findByIdWithUsuarios(chatId)
                    .orElseThrow(() -> semanticError(
                            HttpStatus.NOT_FOUND,
                            Constantes.ERR_NO_ENCONTRADO,
                            Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
            if (!group.isActivo()) {
                throw semanticError(HttpStatus.NOT_FOUND, Constantes.ERR_NO_ENCONTRADO, Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId);
            }
            if (!isActiveGroupMember(group, requesterId) && !requesterIsAdmin) {
                throw semanticError(HttpStatus.FORBIDDEN, Constantes.ERR_CHAT_PIN_FORBIDDEN, "No autorizado para fijar mensajes en este chat");
            }
            return group;
        }

        if (!requesterIsAdmin) {
            throw semanticError(HttpStatus.FORBIDDEN, Constantes.ERR_CHAT_PIN_FORBIDDEN, "No autorizado para fijar mensajes en este chat");
        }
        return chat;
    }

    private long validatePinDurationOrThrow(Long durationSeconds) {
        if (durationSeconds == null) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_CHAT_PIN_INVALID_DURATION, "durationSeconds es obligatorio");
        }
        if (durationSeconds != PIN_DURATION_24H_SECONDS
                && durationSeconds != PIN_DURATION_7D_SECONDS
                && durationSeconds != PIN_DURATION_30D_SECONDS) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_CHAT_PIN_INVALID_DURATION,
                    "durationSeconds debe ser 86400, 604800 o 2592000");
        }
        return durationSeconds;
    }

    private MuteConfig validateMuteRequestOrThrow(ChatMuteRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Payload de mute invalido: mutedForever o durationSeconds es obligatorio");
        }
        boolean mutedForever = Boolean.TRUE.equals(request.getMutedForever());
        Long durationSeconds = request.getDurationSeconds();
        if (mutedForever) {
            return new MuteConfig(true, null);
        }
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds debe ser > 0 cuando mutedForever no es true");
        }
        if (durationSeconds != MUTE_DURATION_8H_SECONDS && durationSeconds != MUTE_DURATION_1W_SECONDS) {
            throw new IllegalArgumentException("durationSeconds solo permite 28800 (8h) o 604800 (1 semana)");
        }
        return new MuteConfig(false, durationSeconds);
    }

    private ChatMuteStateDTO toMuteStateDto(ChatUserStateEntity state, LocalDateTime now) {
        ChatMuteStateDTO dto = new ChatMuteStateDTO();
        dto.setOk(true);
        dto.setChatId(state == null || state.getChat() == null ? null : state.getChat().getId());
        dto.setUserId(state == null || state.getUser() == null ? null : state.getUser().getId());
        boolean active = chatUserStateService.isMuteActive(state, now);
        dto.setMuted(active);
        if (!active) {
            dto.setMutedForever(false);
            dto.setMutedUntil(null);
            return dto;
        }
        dto.setMutedForever(state.isMutedForever());
        dto.setMutedUntil(state.isMutedForever() ? null : state.getMutedUntil());
        return dto;
    }

    private MensajeEntity findActiveMessageForPin(Long chatId, Long messageId, LocalDateTime now) {
        return mensajeRepository.findByIdAndChatId(messageId, chatId)
                .filter(m -> isPinCandidateMessageActive(m, now))
                .orElse(null);
    }

    private boolean isPinCandidateMessageActive(MensajeEntity mensaje, LocalDateTime now) {
        if (mensaje == null || !mensaje.isActivo()) {
            return false;
        }
        LocalDateTime expiresAt = mensaje.getExpiraEn();
        return expiresAt == null || expiresAt.isAfter(now);
    }

    private SemanticApiException semanticError(HttpStatus status, String code, String message) {
        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());
        return new SemanticApiException(status, code, message, traceId);
    }

    private MatchWindow findFirstMatchWindow(String sourceText, String normalizedQuery) {
        if (sourceText == null || sourceText.isBlank() || normalizedQuery == null || normalizedQuery.isBlank()) {
            return null;
        }

        NormalizedText normalizedText = normalizeWithIndexMap(sourceText);
        if (normalizedText.normalized().isBlank() || normalizedText.indexMap().length == 0) {
            return null;
        }

        int normalizedMatchStart = normalizedText.normalized().indexOf(normalizedQuery);
        if (normalizedMatchStart < 0) {
            return null;
        }

        int normalizedMatchEnd = normalizedMatchStart + normalizedQuery.length() - 1;
        if (normalizedMatchEnd >= normalizedText.indexMap().length) {
            return null;
        }

        int originalMatchStart = normalizedText.indexMap()[normalizedMatchStart];
        int originalMatchEndExclusive = Math.min(sourceText.length(), normalizedText.indexMap()[normalizedMatchEnd] + 1);

        int snippetStart = Math.max(0, originalMatchStart - SEARCH_SNIPPET_CONTEXT_CHARS);
        int snippetEnd = Math.min(sourceText.length(), originalMatchEndExclusive + SEARCH_SNIPPET_CONTEXT_CHARS);
        String snippet = sourceText.substring(snippetStart, snippetEnd);
        int snippetMatchStart = Math.max(0, originalMatchStart - snippetStart);
        int snippetMatchEnd = Math.max(snippetMatchStart, originalMatchEndExclusive - snippetStart);

        boolean startsWithQuery = normalizedText.normalized().startsWith(normalizedQuery);
        return new MatchWindow(snippet, snippetMatchStart, snippetMatchEnd, startsWithQuery);
    }

    private NormalizedText normalizeWithIndexMap(String input) {
        if (input == null || input.isEmpty()) {
            return new NormalizedText("", new int[0]);
        }

        StringBuilder normalizedBuilder = new StringBuilder(input.length());
        int[] indexMap = new int[Math.max(16, input.length())];
        int count = 0;

        for (int i = 0; i < input.length(); i++) {
            String normalizedChar = normalizeForSearch(String.valueOf(input.charAt(i)));
            if (normalizedChar.isEmpty()) {
                continue;
            }
            for (int j = 0; j < normalizedChar.length(); j++) {
                if (count == indexMap.length) {
                    indexMap = Arrays.copyOf(indexMap, indexMap.length * 2);
                }
                normalizedBuilder.append(normalizedChar.charAt(j));
                indexMap[count] = i;
                count++;
            }
        }

        return new NormalizedText(
                normalizedBuilder.toString(),
                Arrays.copyOf(indexMap, count));
    }

    private String normalizeForSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
    }

    private MensajeDTO crearMensajeSistemaExpulsion(ChatGrupalEntity chat,
                                                    UsuarioEntity actor,
                                                    UsuarioEntity target) {
        LocalDateTime fechaEnvio = LocalDateTime.now();
        String payload = buildGroupMemberExpelledPayloadJson(chat, actor, target, fechaEnvio);

        MensajeEntity mensaje = new MensajeEntity();
        mensaje.setChat(chat);
        mensaje.setEmisor(actor);
        mensaje.setReceptor(null);
        mensaje.setTipo(MessageType.SYSTEM);
        mensaje.setContenido(payload);
        mensaje.setFechaEnvio(fechaEnvio);
        mensaje.setActivo(true);
        mensaje.setLeido(false);
        mensaje.setReenviado(false);

        MensajeEntity saved = mensajeRepository.save(mensaje);
        MensajeDTO dto = MappingUtils.mensajeEntityADto(saved);

        String actorNombre = buildNombreCompleto(
                actor == null ? null : actor.getNombre(),
                actor == null ? null : actor.getApellido());
        String targetNombre = buildNombreCompleto(
                target == null ? null : target.getNombre(),
                target == null ? null : target.getApellido());
        String groupName = chat == null || chat.getNombreGrupo() == null || chat.getNombreGrupo().isBlank()
                ? "grupo"
                : chat.getNombreGrupo().trim();

        dto.setTipo(Constantes.TIPO_SYSTEM);
        dto.setEsSistema(true);
        dto.setSystemEvent(Constantes.SYSTEM_EVENT_GROUP_MEMBER_EXPELLED);
        dto.setChatId(chat == null ? null : chat.getId());
        dto.setReceptorId(chat == null ? null : chat.getId());
        dto.setTargetUserId(target == null ? null : target.getId());
        dto.setRemovedUserId(target == null ? null : target.getId());
        dto.setTargetUserName(targetNombre);
        dto.setTargetNombreCompleto(targetNombre);
        dto.setGroupName(groupName);
        dto.setNombreGrupo(groupName);
        dto.setEmisorNombre(actor == null ? null : actor.getNombre());
        dto.setEmisorApellido(actor == null ? null : actor.getApellido());
        dto.setEmisorNombreCompleto(actorNombre);
        return dto;
    }

    private String buildGroupMemberExpelledPayloadJson(ChatGrupalEntity chat,
                                                       UsuarioEntity actor,
                                                       UsuarioEntity target,
                                                       LocalDateTime fechaEnvio) {
        String actorNombre = buildNombreCompleto(
                actor == null ? null : actor.getNombre(),
                actor == null ? null : actor.getApellido());
        String targetNombre = buildNombreCompleto(
                target == null ? null : target.getNombre(),
                target == null ? null : target.getApellido());
        String groupName = chat == null || chat.getNombreGrupo() == null || chat.getNombreGrupo().isBlank()
                ? "grupo"
                : chat.getNombreGrupo().trim();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipo", Constantes.TIPO_SYSTEM);
        payload.put(Constantes.KEY_ES_SISTEMA, true);
        payload.put(Constantes.KEY_SYSTEM_EVENT, Constantes.SYSTEM_EVENT_GROUP_MEMBER_EXPELLED);
        payload.put(Constantes.KEY_CHAT_ID, chat == null ? null : chat.getId());
        payload.put("fechaEnvio", fechaEnvio == null ? null : fechaEnvio.toString());
        payload.put(Constantes.KEY_EMISOR_ID, actor == null ? null : actor.getId());
        payload.put(Constantes.KEY_EMISOR_NOMBRE, actor == null ? null : actor.getNombre());
        payload.put(Constantes.KEY_EMISOR_APELLIDO, actor == null ? null : actor.getApellido());
        payload.put("actorNombreCompleto", actorNombre);
        payload.put(Constantes.KEY_TARGET_USER_ID, target == null ? null : target.getId());
        payload.put("removedUserId", target == null ? null : target.getId());
        payload.put("targetUserName", targetNombre);
        payload.put("targetNombreCompleto", targetNombre);
        payload.put("groupName", groupName);
        payload.put("nombreGrupo", groupName);
        payload.put("contenido", Constantes.SYSTEM_EVENT_GROUP_MEMBER_EXPELLED);
        payload.put("textoActor", "Has expulsado a \"" + targetNombre + "\" del grupo \"" + groupName + "\"");
        payload.put("textoExpulsado", "Has sido expulsado de \"" + groupName + "\" por \"" + actorNombre + "\"");
        payload.put("textoTerceros", "\"" + actorNombre + "\" ha expulsado a \"" + targetNombre + "\" del grupo \"" + groupName + "\"");
        return Utils.writeJson(payload);
    }

    private MensajeDTO crearMensajeSistemaSalidaGrupo(ChatGrupalEntity chat, UsuarioEntity usuarioQueSale) {
        String nombreCompleto = buildNombreCompleto(
                usuarioQueSale == null ? null : usuarioQueSale.getNombre(),
                usuarioQueSale == null ? null : usuarioQueSale.getApellido());
        String contenido = nombreCompleto + " ha salido del grupo";

        MensajeEntity mensaje = new MensajeEntity();
        mensaje.setChat(chat);
        mensaje.setEmisor(usuarioQueSale);
        mensaje.setReceptor(null);
        mensaje.setTipo(MessageType.SYSTEM);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setActivo(true);
        mensaje.setLeido(false);
        mensaje.setReenviado(false);

        MensajeEntity saved = mensajeRepository.save(mensaje);
        MensajeDTO dto = MappingUtils.mensajeEntityADto(saved);
        dto.setReceptorId(chat.getId());
        dto.setTipo(Constantes.TIPO_SYSTEM);
        dto.setEsSistema(true);
        dto.setEmisorNombre(usuarioQueSale == null ? null : usuarioQueSale.getNombre());
        dto.setEmisorApellido(usuarioQueSale == null ? null : usuarioQueSale.getApellido());
        dto.setEmisorNombreCompleto(nombreCompleto);

        LOGGER.info(Constantes.LOG_E2E_GROUP_MEMBER_LEFT_SYSTEM_MESSAGE_CREATED,
                chat == null ? null : chat.getId(),
                usuarioQueSale == null ? null : usuarioQueSale.getId(),
                saved.getId(),
                MessageType.SYSTEM.name(),
                E2EDiagnosticUtils.fingerprint12(contenido),
                contenido.length());

        return dto;
    }

    private void applyAdminRawMetadata(ChatResumenDTO dto,
                                       MensajeEntity mensaje,
                                       MensajeTemporalAuditoriaEntity auditoria,
                                       boolean mostrarOriginalAuditoria,
                                       Long requesterId) {
        if (mensaje == null) {
            dto.setUltimoMensaje(Constantes.MSG_SIN_DATOS);
            dto.setUltimoMensajeFecha(null);
            dto.setFechaUltimoMensaje(null);
            dto.setUltimoMensajeTipo(MessageType.TEXT.name());
            dto.setUltimoMensajeTexto(null);
            dto.setUltimoMensajePreview(null);
            dto.setUltimoMensajeDescifrado(null);
            dto.setUltimoMensajeEmisorNombre(null);
            dto.setUltimoMensajeEmisorApellido(null);
            dto.setUltimoMensajeEmisorNombreCompleto(null);
            dto.setUltimoMensajeTemporal(false);
            dto.setUltimoMensajeTemporalSegundos(null);
            dto.setUltimoMensajeExpiraEn(null);
            dto.setUltimoMensajeEstadoTemporal("NO_TEMPORAL");
            return;
        }

        String classification = E2EDiagnosticUtils.analyze(
                mensaje.getContenido(),
                mensaje.getTipo() == null ? null : mensaje.getTipo().name()).getClassification();
        String emisorNombre = mensaje.getEmisor() == null ? null : mensaje.getEmisor().getNombre();
        String emisorApellido = mensaje.getEmisor() == null ? null : mensaje.getEmisor().getApellido();
        String emisorNombreCompleto = buildNombreCompleto(emisorNombre, emisorApellido);
        boolean temporalExpirado = isTemporalExpirado(mensaje);
        String contenidoFallback = temporalExpirado
                ? buildPlaceholderTemporal(mensaje)
                : (mensaje.getContenido() == null ? Constantes.MSG_SIN_DATOS : mensaje.getContenido());
        String rawContenido = contenidoFallback;
        if (temporalExpirado && mostrarOriginalAuditoria && auditoria != null
                && auditoria.getContenidoOriginal() != null && !auditoria.getContenidoOriginal().isBlank()) {
            rawContenido = auditoria.getContenidoOriginal();
            registrarAccesoAuditoria(requesterId, mensaje.getChat() == null ? null : mensaje.getChat().getId(), mensaje.getId());
        }

        dto.setUltimoMensaje(rawContenido);
        dto.setUltimoMensajeFecha(mensaje.getFechaEnvio());
        dto.setFechaUltimoMensaje(mensaje.getFechaEnvio());
        dto.setUltimoMensajeTipo(temporalExpirado
                ? MessageType.TEXT.name()
                : (mensaje.getTipo() == null ? MessageType.TEXT.name() : mensaje.getTipo().name()));
        dto.setUltimoMensajeEmisorNombre(emisorNombre);
        dto.setUltimoMensajeEmisorApellido(emisorApellido);
        dto.setUltimoMensajeEmisorNombreCompleto(emisorNombreCompleto);
        dto.setUltimoMensajeTexto(null);
        dto.setUltimoMensajePreview(null);
        dto.setUltimoMensajeDescifrado(null);
        dto.setUltimoMensajeTemporal(mensaje.isMensajeTemporal());
        dto.setUltimoMensajeTemporalSegundos(mensaje.getMensajeTemporalSegundos());
        dto.setUltimoMensajeExpiraEn(mensaje.getExpiraEn());
        dto.setUltimoMensajeEstadoTemporal(resolveEstadoTemporal(mensaje.isMensajeTemporal(), mensaje.getExpiraEn()));

        logAdminPreviewDiag(mensaje, classification, false, false);
    }

    private Map<Long, MensajeTemporalAuditoriaEntity> cargarAuditoriaPorMensajes(List<MensajeEntity> mensajes) {
        if (mensajes == null || mensajes.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = mensajes.stream()
                .map(MensajeEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mensajeTemporalAuditoriaRepository.findByMensajeIdIn(ids).stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getMensajeId() != null)
                .collect(Collectors.toMap(
                        MensajeTemporalAuditoriaEntity::getMensajeId,
                        a -> a,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private void aplicarCamposAuditoriaAdmin(MensajeDTO dto,
                                             MensajeEntity mensaje,
                                             MensajeTemporalAuditoriaEntity auditoria,
                                             boolean mostrarOriginalAuditoria,
                                             Long requesterId) {
        if (dto == null) {
            return;
        }
        if (auditoria == null) {
            dto.setTieneOriginalAuditoria(false);
            return;
        }
        dto.setTieneOriginalAuditoria(true);
        if (mostrarOriginalAuditoria) {
            dto.setContenidoAuditoria(auditoria.getContenidoOriginal());
            dto.setAudioUrlAuditoria(auditoria.getAudioUrlOriginal());
            dto.setImageUrlAuditoria(auditoria.getImageUrlOriginal());
        }

        if (!mostrarOriginalAuditoria || mensaje == null || !isTemporalExpirado(mensaje)) {
            return;
        }

        if (auditoria.getContenidoOriginal() != null && !auditoria.getContenidoOriginal().isBlank()) {
            dto.setContenido(auditoria.getContenidoOriginal());
        }
        String tipoOriginal = auditoria.getTipoOriginal();
        if (tipoOriginal != null && !tipoOriginal.isBlank()) {
            dto.setTipo(tipoOriginal);
        }
        if (auditoria.getAudioUrlOriginal() != null && !auditoria.getAudioUrlOriginal().isBlank()) {
            dto.setAudioUrl(auditoria.getAudioUrlOriginal());
        }
        if (auditoria.getImageUrlOriginal() != null && !auditoria.getImageUrlOriginal().isBlank()) {
            dto.setImageUrl(auditoria.getImageUrlOriginal());
        }
        dto.setActivo(false);
        dto.setMotivoEliminacion("TEMPORAL_EXPIRADO");
        dto.setEstadoTemporal("EXPIRADO");
        registrarAccesoAuditoria(requesterId, mensaje.getChat() == null ? null : mensaje.getChat().getId(), mensaje.getId());
    }

    private void registrarAccesoAuditoria(Long requesterId, Long chatId, Long mensajeId) {
        LOGGER.info("[AUDITORIA_TEMPORAL_ACCESS] requesterId={} chatId={} mensajeId={} at={}",
                requesterId,
                chatId,
                mensajeId,
                LocalDateTime.now());
    }

    private String resolveEstadoTemporal(boolean mensajeTemporal, LocalDateTime expiraEn) {
        if (!mensajeTemporal || expiraEn == null) {
            return "NO_TEMPORAL";
        }
        return expiraEn.isAfter(LocalDateTime.now()) ? "ACTIVO" : "EXPIRADO";
    }

    private void logAdminPreviewDiag(MensajeEntity mensaje, String classification, boolean decryptOk, boolean usedForAdmin) {
        LOGGER.info(Constantes.LOG_E2E_ADMIN_CHAT_LIST_PREVIEW,
                mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                mensaje.getId(),
                classification,
                decryptOk,
                usedForAdmin);
    }

    private String buildNombreCompleto(String nombre, String apellido) {
        String n = nombre == null ? "" : nombre.trim();
        String a = apellido == null ? "" : apellido.trim();
        String full = (n + (a.isEmpty() ? "" : " " + a)).trim();
        return full.isEmpty() ? Constantes.DEFAULT_CALLER_NAME : full;
    }

    private List<MessageType> parseMediaTypes(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of(MessageType.IMAGE, MessageType.VIDEO, MessageType.AUDIO, MessageType.FILE);
        }
        LinkedHashSet<MessageType> parsed = new LinkedHashSet<>();
        for (String token : csv.split(",")) {
            String normalized = token == null ? "" : token.trim().toUpperCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            MessageType type;
            try {
                type = MessageType.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_TYPES_INVALID_PREFIX + normalized);
            }
            if (!(type == MessageType.IMAGE || type == MessageType.VIDEO || type == MessageType.AUDIO || type == MessageType.FILE)) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_TYPES_INVALID_PREFIX + normalized);
            }
            parsed.add(type);
        }
        if (parsed.isEmpty()) {
            return List.of(MessageType.IMAGE, MessageType.VIDEO, MessageType.AUDIO, MessageType.FILE);
        }
        return new ArrayList<>(parsed);
    }

    private CursorPosition parseCursor(String cursor, Long chatId) {
        if (cursor == null || cursor.isBlank()) {
            return CursorPosition.empty();
        }
        String normalized = cursor.trim();
        if (normalized.contains(CURSOR_SEPARATOR)) {
            String[] parts = normalized.split(CURSOR_SEPARATOR, 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID);
            }
            try {
                long epochMillis = Long.parseLong(parts[0]);
                long id = Long.parseLong(parts[1]);
                LocalDateTime fecha = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
                return new CursorPosition(fecha, id);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID);
            }
        }
        try {
            long messageId = Long.parseLong(normalized);
            MensajeEntity anchor = mensajeRepository.findByIdAndChatId(messageId, chatId)
                    .orElseThrow(() -> new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID));
            return new CursorPosition(anchor.getFechaEnvio(), anchor.getId());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID);
        }
    }

    private String encodeCursor(LocalDateTime fechaEnvio, Long id) {
        if (fechaEnvio == null || id == null) {
            return null;
        }
        long epochMillis = fechaEnvio.toInstant(ZoneOffset.UTC).toEpochMilli();
        return epochMillis + CURSOR_SEPARATOR + id;
    }

    private GroupMediaMeta extractMediaMeta(MensajeEntity mensaje) {
        String mime = mensaje.getMediaMime();
        Integer durMs = mensaje.getMediaDuracionMs();
        Long sizeBytes = mensaje.getMediaSizeBytes();
        String mediaUrl = mensaje.getMediaUrl();
        String fileName = null;

        if (mediaUrl != null && !mediaUrl.isBlank()) {
            int slash = mediaUrl.lastIndexOf('/');
            fileName = slash >= 0 && slash + 1 < mediaUrl.length() ? mediaUrl.substring(slash + 1) : mediaUrl;
            if (sizeBytes == null) {
                sizeBytes = resolveSizeBytesFromMediaUrl(mediaUrl);
            }
        }

        String contenido = mensaje.getContenido();
        if (contenido != null && !contenido.isBlank() && (mime == null || durMs == null || mediaUrl == null || fileName == null || sizeBytes == null)) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(contenido);
                if (mime == null) {
                    mime = firstNonBlank(
                            root.path("audioMime").asText(null),
                            root.path("imageMime").asText(null),
                            root.path("fileMime").asText(null),
                            root.path("mime").asText(null));
                }
                if (durMs == null) {
                    Integer parsedDur = extractInteger(root, "audioDuracionMs");
                    if (parsedDur == null) {
                        parsedDur = extractInteger(root, "durMs");
                    }
                    durMs = parsedDur;
                }
                if (mediaUrl == null) {
                    mediaUrl = firstNonBlank(root.path("audioUrl").asText(null),
                            root.path("imageUrl").asText(null),
                            root.path("fileUrl").asText(null),
                            root.path("mediaUrl").asText(null),
                            root.path("url").asText(null));
                }
                if (fileName == null) {
                    fileName = firstNonBlank(
                            root.path("imageNombre").asText(null),
                            root.path("fileNombre").asText(null),
                            root.path("fileName").asText(null),
                            extractFileNameFromUrl(mediaUrl));
                }
                if (sizeBytes == null) {
                    Long parsedSize = firstNonNull(
                            extractLong(root, "fileSizeBytes"),
                            extractLong(root, "sizeBytes"));
                    if (parsedSize == null && mediaUrl != null) {
                        parsedSize = resolveSizeBytesFromMediaUrl(mediaUrl);
                    }
                    sizeBytes = parsedSize;
                }
            } catch (Exception ignored) {
                // Metadata parsing is best-effort; raw payload stays untouched.
            }
        }

        return new GroupMediaMeta(mime, sizeBytes, durMs, fileName, mediaUrl);
    }

    private Integer extractInteger(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long extractLong(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractFileNameFromUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return null;
        }
        int slash = mediaUrl.lastIndexOf('/');
        return slash >= 0 && slash + 1 < mediaUrl.length() ? mediaUrl.substring(slash + 1) : mediaUrl;
    }

    private Long resolveSizeBytesFromMediaUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank() || !mediaUrl.startsWith(Constantes.UPLOADS_PREFIX)) {
            return null;
        }
        String relative = mediaUrl.substring(Constantes.UPLOADS_PREFIX.length());
        Path absolutePath = Paths.get(uploadsRoot).resolve(relative).normalize();
        try {
            if (Files.exists(absolutePath) && Files.isRegularFile(absolutePath)) {
                return Files.size(absolutePath);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record MuteConfig(boolean mutedForever, Long durationSeconds) {
    }

    private record MatchWindow(String snippet,
                               int matchStart,
                               int matchEnd,
                               boolean startsWithQuery) {
    }

    private record NormalizedText(String normalized, int[] indexMap) {
    }

    private record CursorPosition(LocalDateTime fechaEnvio, Long messageId) {
        static CursorPosition empty() {
            return new CursorPosition(null, null);
        }
    }

    private record GroupMemberExpelledPayload(Long actorId,
                                              Long targetId,
                                              String actorName,
                                              String targetName,
                                              String groupName) {
    }

    private boolean esAdminOAuditor(UsuarioEntity usuario) {
        if (securityUtils.hasRole(Constantes.ADMIN)
                || securityUtils.hasRole(Constantes.ROLE_ADMIN)
                || securityUtils.hasRole("AUDITOR")
                || securityUtils.hasRole("ROLE_AUDITOR")) {
            return true;
        }
        if (usuario == null || usuario.getRoles() == null) {
            return false;
        }
        return usuario.getRoles().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(rol -> Constantes.ADMIN.equalsIgnoreCase(rol)
                        || Constantes.ROLE_ADMIN.equalsIgnoreCase(rol)
                        || "AUDITOR".equalsIgnoreCase(rol)
                        || "ROLE_AUDITOR".equalsIgnoreCase(rol));
    }

    private void validateSelfOrAdminAuditor(Long targetUserId) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        if (Objects.equals(requesterId, targetUserId)) {
            return;
        }
        boolean requesterPuedeAuditar = usuarioRepo.findById(requesterId)
                .map(this::esAdminOAuditor)
                .orElse(false);
        if (!requesterPuedeAuditar) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }
    }

    private void validateAdminOnly() {
        if (securityUtils.hasRole(Constantes.ADMIN) || securityUtils.hasRole(Constantes.ROLE_ADMIN)) {
            return;
        }
        Long requesterId = securityUtils.getAuthenticatedUserId();
        boolean isAdmin = usuarioRepo.findById(requesterId)
                .map(u -> u.getRoles() != null && u.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .anyMatch(rol -> Constantes.ADMIN.equalsIgnoreCase(rol) || Constantes.ROLE_ADMIN.equalsIgnoreCase(rol)))
                .orElse(false);
        if (!isAdmin) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }
    }

    private void validateBulkEmailRequest(BulkEmailRequestDTO request, List<MultipartFile> attachments) {
        if (request == null) {
            throw new IllegalArgumentException("payload es obligatorio");
        }

        List<String> errors = new ArrayList<>();
        List<String> recipientEmails = request.getRecipientEmails() == null ? List.of() : request.getRecipientEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        if (recipientEmails.isEmpty()) {
            errors.add("recipientEmails no puede estar vacio");
        }
        if (!StringUtils.hasText(request.getSubject())) {
            errors.add("subject no puede estar vacio");
        }
        if (!StringUtils.hasText(request.getBody())) {
            errors.add("body no puede estar vacio");
        }

        int actualAttachmentCount = attachments == null ? 0 : (int) attachments.stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .count();
        int declaredAttachmentCount = request.getAttachmentCount() == null ? 0 : request.getAttachmentCount();
        if (declaredAttachmentCount != actualAttachmentCount) {
            errors.add("attachmentCount no coincide con attachments recibidos");
        }

        if (request.getAttachmentCount() != null && request.getAttachmentCount() < 0) {
            errors.add("attachmentCount no puede ser negativo");
        }

        if (!errors.isEmpty()) {
            throw new ValidacionPayloadException("Bulk email payload invalido", errors);
        }
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

    private String resolveSafeBulkEmailError(Exception ex) {
        String message = ex == null ? null : ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "Email send failed";
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private ChatCloseStateDTO toChatCloseState(ChatGrupalEntity chat) {
        ChatCloseStateDTO dto = new ChatCloseStateDTO();
        dto.setOk(true);
        dto.setChatId(chat == null ? null : chat.getId());
        boolean closed = chat != null && chat.isClosed();
        dto.setClosed(closed);
        dto.setCerrado(closed);
        dto.setReason(closed && chat != null ? chat.getClosedReason() : null);
        dto.setMotivo(closed && chat != null ? chat.getClosedReason() : null);
        dto.setClosedAt(closed && chat != null ? chat.getClosedAt() : null);
        dto.setClosedByAdminId(closed && chat != null ? chat.getClosedByAdminId() : null);
        return dto;
    }

    private void emitChatClosureEvent(ChatGrupalEntity chat, ChatCloseStateDTO state) {
        if (chat == null || chat.getUsuarios() == null || state == null) {
            return;
        }
        ChatClosureEventDTO event = new ChatClosureEventDTO();
        event.setChatId(state.getChatId());
        event.setClosed(state.isClosed());
        event.setCerrado(state.isCerrado());
        event.setReason(state.getReason());
        event.setMotivo(state.getMotivo());
        event.setClosedAt(state.getClosedAt());
        event.setClosedByAdminId(state.getClosedByAdminId());

        chat.getUsuarios().stream()
                .filter(Objects::nonNull)
                .filter(UsuarioEntity::isActivo)
                .forEach(u -> {
                    if (u.getEmail() == null || u.getEmail().isBlank()) {
                        return;
                    }
                    messagingTemplate.convertAndSendToUser(
                            u.getEmail(),
                            Constantes.WS_QUEUE_CHAT_CIERRES,
                            event);
                });
    }

    private String normalizeCloseReason(String reason, boolean applyDefaultWhenBlank) {
        if (reason == null) {
            return applyDefaultWhenBlank ? DEFAULT_CLOSE_REASON : null;
        }
        if (reason.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("motivo contiene null bytes no permitidos");
        }
        String sanitized = UNSAFE_CONTROL_CHARS.matcher(reason).replaceAll(" ").trim();
        if (sanitized.isEmpty()) {
            return applyDefaultWhenBlank ? DEFAULT_CLOSE_REASON : null;
        }
        if (sanitized.length() > MAX_CLOSE_REASON_LENGTH) {
            throw new IllegalArgumentException("motivo excede longitud maxima de " + MAX_CLOSE_REASON_LENGTH + " caracteres");
        }
        return sanitized;
    }

    private String safeAuditValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 256) {
            return trimmed;
        }
        return trimmed.substring(0, 256);
    }


}

