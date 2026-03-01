package com.chat.chat.Service.MensajeriaService;

import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.DTO.MensajeReaccionDTO;
import com.chat.chat.Entity.ChatEntity;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.MensajeReaccionEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.E2EGroupValidationException;
import com.chat.chat.Exceptions.ReenvioInvalidoException;
import com.chat.chat.Exceptions.ReenvioNoAutorizadoException;
import com.chat.chat.Exceptions.RespuestaInvalidaException;
import com.chat.chat.Exceptions.RespuestaNoAutorizadaException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeReaccionRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.E2EDiagnosticUtils;
import com.chat.chat.Utils.E2EGroupValidationUtils;
import com.chat.chat.Utils.E2EPayloadUtils;
import com.chat.chat.Utils.ExceptionConstants;
import com.chat.chat.Utils.ReactionAction;
import com.chat.chat.Utils.SecurityUtils;
import com.chat.chat.Utils.Utils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MensajeriaServiceImpl implements MensajeriaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MensajeriaServiceImpl.class);

    @Value(Constantes.PROP_UPLOADS_ROOT)
    private String uploadsRoot;

    @Value(Constantes.PROP_UPLOADS_BASE_URL)
    private String uploadsBaseUrl;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private MensajeReaccionRepository mensajeReaccionRepository;

    @Autowired
    private ChatIndividualRepository chatIndividualRepository;

    @Autowired
    private ChatGrupalRepository chatGrupalRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Override
    @Transactional
    public MensajeDTO guardarMensajeIndividual(MensajeDTO dto) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        System.out.println(
                Constantes.LOG_GUARDANDO_MSG_INDIVIDUAL + authenticatedUserId + Constantes.LOG_RECEPTOR + dto.getReceptorId());

        UsuarioEntity emisor = usuarioRepository.findById(authenticatedUserId).orElseThrow();
        UsuarioEntity receptor = usuarioRepository.findById(dto.getReceptorId()).orElseThrow();

        ChatIndividualEntity chat = chatIndividualRepository.findByUsuario1AndUsuario2(emisor, receptor)
                .or(() -> chatIndividualRepository.findByUsuario1AndUsuario2(receptor, emisor))
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_INDIVIDUAL_NO_ENCONTRADO));

        normalizeReenvio(dto, authenticatedUserId);
        normalizeRespuesta(dto, authenticatedUserId, chat);

        if (emisor.getBloqueados().contains(receptor) || receptor.getBloqueados().contains(emisor)) {
            throw new RuntimeException(Constantes.MSG_NO_PUEDE_ENVIAR_MENSAJES);
        }

        // === AUDIO ===
        if (dto.getAudioDataUrl() != null && dto.getAudioDataUrl().startsWith(Constantes.DATA_AUDIO_PREFIX)) {
            // Guardar a disco (voice/)
            String publicUrl = Utils.saveDataUrlToUploads(dto.getAudioDataUrl(), Constantes.DIR_VOICE, uploadsRoot, uploadsBaseUrl);
            dto.setAudioUrl(publicUrl);
            // inferir mime del dataURL
            String mime = dto.getAudioDataUrl().substring(5, dto.getAudioDataUrl().indexOf(';')); // "audio/webm"
            dto.setAudioMime(mime);
            dto.setTipo(Constantes.TIPO_AUDIO);
        } else if (dto.getAudioUrl() != null && !dto.getAudioUrl().isBlank()) {
            dto.setTipo(Constantes.TIPO_AUDIO);
        } else if (dto.getTipo() == null || dto.getTipo().isBlank()) {
            // si no hay indicios de audio ni tipo explicito, asumimos texto
            dto.setTipo(Constantes.TIPO_TEXT);
        }

        boolean imageType = E2EGroupValidationUtils.isImageType(dto.getTipo());
        E2EDiagnosticUtils.ContentDiagnostic inboundDiag = E2EDiagnosticUtils.analyze(dto.getContenido(), dto.getTipo());
        boolean encryptedAudio = E2EGroupValidationUtils.isAudioType(dto.getTipo())
                && E2EGroupValidationUtils.isE2EAudio(inboundDiag);
        if (encryptedAudio && !E2EGroupValidationUtils.hasRequiredE2EAudioFields(dto.getContenido())) {
            LOGGER.warn(
                    Constantes.LOG_E2E_INBOUND_INDIVIDUAL_AUDIO_REJECT,
                    Instant.now(),
                    authenticatedUserId,
                    dto.getReceptorId(),
                    dto.getTipo(),
                    inboundDiag.getClassification(),
                    inboundDiag.getHash12(),
                    Constantes.ERR_E2E_AUDIO_PAYLOAD_INVALID);
            throw new E2EGroupValidationException(
                    Constantes.ERR_E2E_AUDIO_PAYLOAD_INVALID,
                    ExceptionConstants.ERROR_E2E_AUDIO_PAYLOAD_INVALID);
        }
        if (imageType && !E2EGroupValidationUtils.hasRequiredE2EImageFields(dto.getContenido())) {
            throw new E2EGroupValidationException(
                    Constantes.ERR_E2E_IMAGE_PAYLOAD_INVALID,
                    ExceptionConstants.ERROR_E2E_IMAGE_PAYLOAD_INVALID);
        }

        MensajeEntity mensaje = MappingUtils.mensajeDtoAEntity(dto, emisor, receptor);
        if (imageType) {
            mensaje.setContenido(mensaje.getContenido());
        } else {
            mensaje.setContenido(E2EPayloadUtils.normalizeForStorage(mensaje.getContenido()));
        }
        mensaje.setChat(chat);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setActivo(true);
        mensaje.setLeido(false);

        MensajeEntity saved = mensajeRepository.save(mensaje);
        return MappingUtils.mensajeEntityADto(saved);
    }

    @Override
    @Transactional
    public MensajeDTO guardarMensajeGrupal(MensajeDTO dto) {
        String traceId = E2EDiagnosticUtils.currentTraceId();
        boolean createdTraceId = false;
        if (traceId == null) {
            traceId = E2EDiagnosticUtils.newTraceId();
            MDC.put(E2EDiagnosticUtils.TRACE_ID_MDC_KEY, traceId);
            createdTraceId = true;
        }

        Long chatId = dto.getChatId() != null ? dto.getChatId() : dto.getReceptorId();
        E2EDiagnosticUtils.ContentDiagnostic inboundDiag = E2EDiagnosticUtils.analyze(dto.getContenido(), dto.getTipo());

        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        try {
            UsuarioEntity emisor = usuarioRepository.findById(authenticatedUserId).orElseThrow();

            // dto.receptorId llega con el id del chat grupal
            ChatGrupalEntity chatGrupal = chatGrupalRepository.findById(chatId)
                    .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO));

            List<Long> memberIdsAtSend = E2EGroupValidationUtils.activeMemberIds(chatGrupal);
            List<Long> expectedRecipientIds = E2EGroupValidationUtils.expectedActiveRecipientIds(chatGrupal, authenticatedUserId);
            Set<String> forReceptoresKeysInPayload = E2EDiagnosticUtils.extractForReceptoresKeys(dto.getContenido());
            Set<String> expectedRecipientKeys = expectedRecipientIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Map<Long, String> recipientPublicKeyFp = new LinkedHashMap<>();
            boolean senderKeyPresent = E2EGroupValidationUtils.hasPublicKey(emisor.getPublicKey());
            boolean textType = E2EGroupValidationUtils.isTextType(dto == null ? null : dto.getTipo());
            boolean audioType = E2EGroupValidationUtils.isAudioType(dto == null ? null : dto.getTipo());
            boolean imageType = E2EGroupValidationUtils.isImageType(dto == null ? null : dto.getTipo());
            boolean encryptedGroupAudio = audioType && E2EGroupValidationUtils.isE2EGroupAudio(inboundDiag);
            for (Long recipientId : expectedRecipientIds) {
                String recipientKey = usuarioRepository.findFreshById(recipientId)
                        .map(UsuarioEntity::getPublicKey)
                        .orElse(null);
                recipientPublicKeyFp.put(recipientId, E2EDiagnosticUtils.fingerprint12(recipientKey));
            }
            LOGGER.info(
                    Constantes.LOG_E2E_GROUP_RECIPIENTS_BUILD,
                    Instant.now(),
                    traceId,
                    dto.getId(),
                    chatId,
                    authenticatedUserId,
                    memberIdsAtSend,
                    expectedRecipientIds,
                    forReceptoresKeysInPayload,
                    E2EDiagnosticUtils.fingerprint12(emisor.getPublicKey()),
                    recipientPublicKeyFp);

            boolean recipientKeysMatch = expectedRecipientKeys.equals(forReceptoresKeysInPayload);
            boolean groupAudioPayloadValid = !encryptedGroupAudio
                    || E2EGroupValidationUtils.hasRequiredE2EGroupAudioFields(dto.getContenido());
            boolean groupImagePayloadValid = !imageType
                    || E2EGroupValidationUtils.hasRequiredE2EGroupImageFields(dto.getContenido());
            boolean groupValidateOk = true;
            String inboundValidateRejectReason = "-";
            if (textType && !recipientKeysMatch) {
                groupValidateOk = false;
                inboundValidateRejectReason = Constantes.ERR_E2E_RECIPIENT_KEYS_MISMATCH;
            }
            if (encryptedGroupAudio && !groupAudioPayloadValid) {
                groupValidateOk = false;
                inboundValidateRejectReason = Constantes.ERR_E2E_GROUP_AUDIO_PAYLOAD_INVALID;
            } else if (encryptedGroupAudio && !recipientKeysMatch) {
                groupValidateOk = false;
                inboundValidateRejectReason = Constantes.ERR_E2E_AUDIO_RECIPIENT_KEYS_MISMATCH;
            }
            if (imageType && !groupImagePayloadValid) {
                groupValidateOk = false;
                inboundValidateRejectReason = Constantes.ERR_E2E_GROUP_IMAGE_PAYLOAD_INVALID;
            } else if (imageType && !recipientKeysMatch) {
                groupValidateOk = false;
                inboundValidateRejectReason = Constantes.ERR_E2E_RECIPIENT_KEYS_MISMATCH;
            }
            LOGGER.info(Constantes.LOG_E2E_INBOUND_GROUP_VALIDATE,
                    Instant.now(),
                    traceId,
                    chatId,
                    authenticatedUserId,
                    expectedRecipientIds,
                    forReceptoresKeysInPayload,
                    groupValidateOk,
                    inboundValidateRejectReason);
            if (audioType) {
                LOGGER.info(Constantes.LOG_E2E_INBOUND_GROUP_AUDIO_VALIDATE,
                        Instant.now(),
                        traceId,
                        chatId,
                        dto.getId(),
                        authenticatedUserId,
                        dto.getTipo(),
                        inboundDiag.getClassification(),
                        inboundDiag.getHash12(),
                        inboundDiag.getForReceptoresKeys(),
                        expectedRecipientIds,
                        forReceptoresKeysInPayload,
                        !encryptedGroupAudio || groupValidateOk,
                        encryptedGroupAudio ? inboundValidateRejectReason : "-");
            }
            if (encryptedGroupAudio && !groupAudioPayloadValid) {
                throw new E2EGroupValidationException(
                        Constantes.ERR_E2E_GROUP_AUDIO_PAYLOAD_INVALID,
                        ExceptionConstants.ERROR_E2E_GROUP_AUDIO_PAYLOAD_INVALID);
            }
            if (textType && !recipientKeysMatch) {
                throw new E2EGroupValidationException(
                        Constantes.ERR_E2E_RECIPIENT_KEYS_MISMATCH,
                        ExceptionConstants.ERROR_E2E_RECIPIENTS_MISMATCH);
            }
            if (encryptedGroupAudio && !recipientKeysMatch) {
                throw new E2EGroupValidationException(
                        Constantes.ERR_E2E_AUDIO_RECIPIENT_KEYS_MISMATCH,
                        ExceptionConstants.ERROR_E2E_GROUP_AUDIO_RECIPIENTS_MISMATCH);
            }
            if (imageType && !groupImagePayloadValid) {
                throw new E2EGroupValidationException(
                        Constantes.ERR_E2E_GROUP_IMAGE_PAYLOAD_INVALID,
                        ExceptionConstants.ERROR_E2E_GROUP_IMAGE_PAYLOAD_INVALID);
            }
            if (imageType && !recipientKeysMatch) {
                throw new E2EGroupValidationException(
                        Constantes.ERR_E2E_RECIPIENT_KEYS_MISMATCH,
                        ExceptionConstants.ERROR_E2E_RECIPIENTS_MISMATCH);
            }

            normalizeReenvio(dto, authenticatedUserId);
            normalizeRespuesta(dto, authenticatedUserId, chatGrupal);

            MensajeEntity mensaje = MappingUtils.mensajeDtoAEntity(dto, emisor, null);
            String rawContent = mensaje.getContenido();
            String normalizedContent = imageType ? rawContent : E2EPayloadUtils.normalizeForStorage(rawContent);
            boolean transformed = !Objects.equals(rawContent, normalizedContent);
            E2EDiagnosticUtils.ContentDiagnostic prePersistDiag = E2EDiagnosticUtils.analyze(
                    normalizedContent,
                    String.valueOf(mensaje.getTipo()));

            if (!senderKeyPresent) {
                LOGGER.warn(
                        Constantes.LOG_E2E_PRE_PERSIST_REJECT,
                        Instant.now(),
                        traceId,
                        chatId,
                        authenticatedUserId,
                        dto.getReceptorId(),
                        mensaje.getTipo(),
                        inboundDiag.getClassification(),
                        inboundDiag.getLength(),
                        inboundDiag.getHash12(),
                        prePersistDiag.getClassification(),
                        prePersistDiag.getLength(),
                        prePersistDiag.getHash12(),
                        transformed,
                        false,
                        expectedRecipientIds,
                        forReceptoresKeysInPayload,
                        Constantes.ERR_E2E_SENDER_KEY_MISSING);
                throw new E2EGroupValidationException(Constantes.ERR_E2E_SENDER_KEY_MISSING, ExceptionConstants.ERROR_E2E_SENDER_PUBLIC_KEY_MISSING);
            }

            LOGGER.info(
                    Constantes.LOG_E2E_PRE_PERSIST,
                    Instant.now(),
                    traceId,
                    chatId,
                    authenticatedUserId,
                    dto.getReceptorId(),
                    mensaje.getTipo(),
                    inboundDiag.getClassification(),
                    inboundDiag.getLength(),
                    inboundDiag.getHash12(),
                    prePersistDiag.getClassification(),
                    prePersistDiag.getLength(),
                    prePersistDiag.getHash12(),
                    transformed,
                    prePersistDiag.hasIv(),
                    prePersistDiag.hasCiphertext(),
                    prePersistDiag.hasForEmisor(),
                    prePersistDiag.hasForReceptores(),
                    prePersistDiag.hasForAdmin(),
                    prePersistDiag.getForReceptoresKeys(),
                    senderKeyPresent,
                    expectedRecipientIds,
                    forReceptoresKeysInPayload,
                    "-");
            if ("INVALID_JSON".equals(inboundDiag.getClassification())) {
                LOGGER.warn(Constantes.LOG_E2E_PRE_PERSIST_PARSE_WARN,
                        Instant.now(), traceId, chatId, dto.getId(), inboundDiag.getParseErrorClass());
            }
            if ("INVALID_JSON".equals(prePersistDiag.getClassification())) {
                LOGGER.warn(Constantes.LOG_E2E_PRE_PERSIST_NORMALIZED_PARSE_WARN,
                        Instant.now(), traceId, chatId, dto.getId(), prePersistDiag.getParseErrorClass());
            }

            mensaje.setContenido(normalizedContent);
            mensaje.setChat(chatGrupal);
            mensaje.setFechaEnvio(LocalDateTime.now());
            mensaje.setActivo(true);
            mensaje.setLeido(false);

            MensajeEntity saved = mensajeRepository.save(mensaje);
            E2EDiagnosticUtils.ContentDiagnostic postPersistDiag = E2EDiagnosticUtils.analyze(
                    saved.getContenido(),
                    String.valueOf(saved.getTipo()));
            LOGGER.info(
                    Constantes.LOG_E2E_POST_PERSIST,
                    Instant.now(),
                    traceId,
                    chatId,
                    saved.getId(),
                    authenticatedUserId,
                    saved.getTipo(),
                    postPersistDiag.getClassification(),
                    postPersistDiag.getLength(),
                    postPersistDiag.getHash12(),
                    postPersistDiag.hasIv(),
                    postPersistDiag.hasCiphertext(),
                    postPersistDiag.hasForEmisor(),
                    postPersistDiag.hasForReceptores(),
                    postPersistDiag.hasForAdmin(),
                    postPersistDiag.getForReceptoresKeys(),
                    senderKeyPresent,
                    expectedRecipientIds,
                    forReceptoresKeysInPayload,
                    recipientPublicKeyFp);
            if ("INVALID_JSON".equals(postPersistDiag.getClassification())) {
                LOGGER.warn(Constantes.LOG_E2E_POST_PERSIST_PARSE_WARN,
                        Instant.now(), traceId, chatId, saved.getId(), postPersistDiag.getParseErrorClass());
            }

            MensajeDTO out = MappingUtils.mensajeEntityADto(saved);

            // Enriquecer con datos del emisor para que el front no tenga que resolver nada
            out.setEmisorNombre(emisor.getNombre());
            out.setEmisorApellido(emisor.getApellido());
            if (emisor.getFotoUrl() != null) {
                out.setEmisorFoto(Utils.toDataUrlFromUrl(emisor.getFotoUrl(), uploadsRoot)); // o devuelve URL si prefieres
            }
            return out;
        } catch (RuntimeException ex) {
            String rejectReason = ex instanceof E2EGroupValidationException
                    ? ((E2EGroupValidationException) ex).getCode()
                    : "-";
            LOGGER.error(Constantes.LOG_E2E_PERSIST_ERROR,
                    Instant.now(), traceId, chatId, dto.getId(), ex.getClass().getSimpleName(), rejectReason);
            throw ex;
        } finally {
            if (createdTraceId) {
                MDC.remove(E2EDiagnosticUtils.TRACE_ID_MDC_KEY);
            }
        }
    }

    @Override
    @Transactional
    public ReactionDispatchResult procesarReaccion(MensajeReaccionDTO request) {
        validateReaccionRequest(request);

        Long authUserId = securityUtils.getAuthenticatedUserId();
        if (!Objects.equals(authUserId, request.getReactorUserId())) {
            throw new AccessDeniedException("reactorUserId no coincide con el usuario autenticado");
        }

        MensajeEntity mensaje = mensajeRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("messageId no existe"));
        if (!mensaje.isActivo()) {
            throw new IllegalArgumentException("No se puede reaccionar a un mensaje inactivo");
        }
        if (mensaje.getChat() == null || !Objects.equals(mensaje.getChat().getId(), request.getChatId())) {
            throw new IllegalArgumentException("chatId no coincide con el mensaje");
        }

        UsuarioEntity reactor = usuarioRepository.findById(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("reactorUserId no existe"));
        ReactionAction action = request.actionAsEnumOrNull();
        if (action == null) {
            throw new IllegalArgumentException("action debe ser SET o REMOVE");
        }
        String emojiNormalized = MensajeReaccionDTO.normalizeEmoji(request.getEmoji());

        ChatEntity chat = mensaje.getChat();
        Set<Long> recipients = new LinkedHashSet<>();
        Long targetUserId = null;
        boolean groupChat;

        if (chat instanceof ChatIndividualEntity) {
            ChatIndividualEntity chatIndividual = chatIndividualRepository.findById(chat.getId())
                    .orElseThrow(() -> new IllegalArgumentException("chat individual no existe"));
            Long user1 = chatIndividual.getUsuario1() == null ? null : chatIndividual.getUsuario1().getId();
            Long user2 = chatIndividual.getUsuario2() == null ? null : chatIndividual.getUsuario2().getId();

            boolean reactorBelongs = Objects.equals(user1, authUserId) || Objects.equals(user2, authUserId);
            if (!reactorBelongs) {
                throw new AccessDeniedException("reactor no pertenece al chat individual");
            }

            Long derivedTarget = Objects.equals(authUserId, user1) ? user2 : user1;
            if (request.getTargetUserId() != null && !Objects.equals(request.getTargetUserId(), derivedTarget)) {
                throw new IllegalArgumentException("targetUserId no pertenece al chat individual");
            }
            if (user1 != null) {
                recipients.add(user1);
            }
            if (user2 != null) {
                recipients.add(user2);
            }
            targetUserId = derivedTarget;
            groupChat = false;
        } else if (chat instanceof ChatGrupalEntity) {
            ChatGrupalEntity chatGrupal = chatGrupalRepository.findByIdWithUsuarios(chat.getId())
                    .orElseThrow(() -> new IllegalArgumentException("chat grupal no existe"));
            if (!chatGrupal.isActivo()) {
                throw new IllegalArgumentException("chat grupal inactivo");
            }
            if (request.getTargetUserId() != null) {
                throw new IllegalArgumentException("targetUserId debe ser null en chat grupal");
            }

            boolean reactorBelongs = chatGrupal.getUsuarios() != null && chatGrupal.getUsuarios().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(u -> Objects.equals(u.getId(), authUserId) && u.isActivo());
            if (!reactorBelongs) {
                throw new AccessDeniedException("reactor no pertenece al grupo");
            }

            if (chatGrupal.getUsuarios() != null) {
                chatGrupal.getUsuarios().stream()
                        .filter(Objects::nonNull)
                        .filter(UsuarioEntity::isActivo)
                        .map(UsuarioEntity::getId)
                        .filter(Objects::nonNull)
                        .forEach(recipients::add);
            }
            targetUserId = null;
            groupChat = true;
        } else {
            throw new IllegalArgumentException("Tipo de chat no soportado para reaccion");
        }

        Optional<MensajeReaccionEntity> existingOpt = mensajeReaccionRepository
                .findByMensajeIdAndUsuarioId(mensaje.getId(), authUserId);

        String normalizedAction = action.name();
        String normalizedEmojiOut = null;
        LocalDateTime createdAt = null;

        if (action == ReactionAction.SET) {
            if (emojiNormalized == null) {
                throw new IllegalArgumentException("emoji es obligatorio para action=SET");
            }
            if (existingOpt.isPresent()) {
                MensajeReaccionEntity existing = existingOpt.get();
                if (Objects.equals(existing.getEmoji(), emojiNormalized)) {
                    normalizedEmojiOut = existing.getEmoji();
                    createdAt = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                } else {
                    existing.setEmoji(emojiNormalized);
                    MensajeReaccionEntity saved = mensajeReaccionRepository.save(existing);
                    normalizedEmojiOut = saved.getEmoji();
                    createdAt = saved.getUpdatedAt() != null ? saved.getUpdatedAt() : saved.getCreatedAt();
                }
            } else {
                MensajeReaccionEntity entity = new MensajeReaccionEntity();
                entity.setMensaje(mensaje);
                entity.setUsuario(reactor);
                entity.setEmoji(emojiNormalized);
                MensajeReaccionEntity saved = mensajeReaccionRepository.save(entity);
                normalizedEmojiOut = saved.getEmoji();
                createdAt = saved.getUpdatedAt() != null ? saved.getUpdatedAt() : saved.getCreatedAt();
            }
        } else {
            existingOpt.ifPresent(mensajeReaccionRepository::delete);
            normalizedAction = ReactionAction.REMOVE.name();
            normalizedEmojiOut = null;
            createdAt = null;
        }

        MensajeReaccionDTO event = new MensajeReaccionDTO();
        event.setEvent(MensajeReaccionDTO.EVENT_MESSAGE_REACTION);
        event.setMessageId(mensaje.getId());
        event.setChatId(chat.getId());
        event.setEsGrupo(groupChat);
        event.setReactorUserId(authUserId);
        event.setTargetUserId(targetUserId);
        event.setEmoji(normalizedEmojiOut);
        event.setAction(normalizedAction);
        event.setCreatedAt(createdAt);

        return new ReactionDispatchResult(event, recipients, chat.getId(), groupChat);
    }

    @Override
    @Transactional
    public void marcarMensajesComoLeidos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.info(Constantes.LOG_WS_MARK_READ_NO_MSG, ids);
            return;
        }

        List<Long> uniqueIds = new java.util.ArrayList<>(new LinkedHashSet<>(ids));
        int updated = mensajeRepository.markLeidoByIds(uniqueIds);
        if (updated <= 0) {
            LOGGER.info(Constantes.LOG_WS_MARK_READ_NO_MSG, uniqueIds);
            return;
        }

        List<MensajeEntity> mensajes = mensajeRepository.findAllById(uniqueIds);
        if (mensajes == null || mensajes.isEmpty()) {
            LOGGER.info(Constantes.LOG_WS_MARK_READ_NO_MSG, uniqueIds);
            return;
        }

        // Notificar por WebSocket al emisor de cada mensaje
        mensajes.forEach(mensaje -> {
            if (mensaje.getEmisor() == null || mensaje.getEmisor().getId() == null) {
                LOGGER.warn(Constantes.LOG_WS_MARK_READ_NO_EMISOR, mensaje.getId());
                return;
            }
            Long emisorId = mensaje.getEmisor().getId();
            Map<String, Long> payload = new HashMap<>();
            payload.put(Constantes.KEY_MENSAJE_ID, mensaje.getId());

            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_LEIDO + emisorId, payload);
            LOGGER.info(Constantes.LOG_WS_SEND_LEIDO, emisorId, mensaje.getId());
        });
    }

    @Override
    @Transactional
    public boolean eliminarMensajePropio(MensajeDTO mensajeDTO) {
        if (mensajeDTO == null || mensajeDTO.getId() == null) {
            LOGGER.warn(Constantes.LOG_WS_DELETE_INVALID);
            return false;
        }
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        LOGGER.info(Constantes.LOG_WS_DELETE, authenticatedUserId, mensajeDTO);
        Optional<Long> emisorIdOpt = mensajeRepository.findEmisorIdById(mensajeDTO.getId());

        if (emisorIdOpt.isPresent()) {
            Long emisorId = emisorIdOpt.get();
            LOGGER.info(Constantes.LOG_DELETE_MSG_STATE, mensajeDTO.getId(), emisorId, false);

            // Validar que el mensaje pertenece al emisor autenticado
            if (!emisorId.equals(authenticatedUserId)) {
                LOGGER.warn(Constantes.LOG_DELETE_MSG_NOT_OWNER, mensajeDTO.getId(), emisorId, authenticatedUserId);
                return false;
            }

            int updated = mensajeRepository.markInactivoById(mensajeDTO.getId());
            return updated > 0;
        }
        LOGGER.warn(Constantes.LOG_DELETE_MSG_NOT_FOUND, mensajeDTO.getId());
        return false;
    }

    private void validateReaccionRequest(MensajeReaccionDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("payload de reaccion vacio");
        }
        List<String> errors = new ArrayList<>(request.validarEntrada());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Payload de reaccion invalido: " + String.join("; ", errors));
        }
    }

    private void normalizeReenvio(MensajeDTO dto, Long authenticatedUserId) {
        if (dto == null) {
            return;
        }
        if (dto.isReenviado()) {
            Long originalId = dto.getMensajeOriginalId();
            if (originalId == null) {
                throw new ReenvioInvalidoException(ExceptionConstants.ERROR_REENVIO_ID_REQUERIDO);
            }
            MensajeEntity original = mensajeRepository.findById(originalId)
                    .orElseThrow(() -> new ReenvioInvalidoException(
                            ExceptionConstants.ERROR_REENVIO_ORIGINAL_NO_EXISTE + originalId));
            UsuarioEntity usuario = usuarioRepository.findById(authenticatedUserId).orElseThrow();
            if (!canAccessChat(usuario, original.getChat())) {
                throw new ReenvioNoAutorizadoException(ExceptionConstants.ERROR_REENVIO_NO_AUTORIZADO);
            }
        } else {
            dto.setMensajeOriginalId(null);
        }
    }

    private void normalizeRespuesta(MensajeDTO dto, Long authenticatedUserId, ChatEntity chatDestino) {
        if (dto == null) {
            return;
        }
        Long replyId = dto.getReplyToMessageId();
        if (replyId == null) {
            dto.setReplySnippet(null);
            dto.setReplyAuthorName(null);
            return;
        }

        MensajeEntity original = mensajeRepository.findById(replyId)
                .orElseThrow(() -> new RespuestaInvalidaException(ExceptionConstants.ERROR_RESPUESTA_INVALIDA));

        if (!original.isActivo()) {
            throw new RespuestaInvalidaException(ExceptionConstants.ERROR_RESPUESTA_INVALIDA);
        }

        UsuarioEntity usuario = usuarioRepository.findById(authenticatedUserId).orElseThrow();
        if (!canAccessChat(usuario, original.getChat())) {
            throw new RespuestaNoAutorizadaException(ExceptionConstants.ERROR_RESPUESTA_NO_AUTORIZADA);
        }

        if (original.getChat() == null || chatDestino == null
                || !Objects.equals(original.getChat().getId(), chatDestino.getId())) {
            throw new RespuestaInvalidaException(ExceptionConstants.ERROR_RESPUESTA_INVALIDA);
        }

    }

    private boolean canAccessChat(UsuarioEntity usuario, ChatEntity chat) {
        if (usuario == null || chat == null || usuario.getId() == null) {
            return false;
        }
        if (chat instanceof ChatIndividualEntity) {
            ChatIndividualEntity ci = (ChatIndividualEntity) chat;
            Long userId = usuario.getId();
            return (ci.getUsuario1() != null && Objects.equals(ci.getUsuario1().getId(), userId))
                    || (ci.getUsuario2() != null && Objects.equals(ci.getUsuario2().getId(), userId));
        }
        if (chat instanceof ChatGrupalEntity) {
            ChatGrupalEntity cg = (ChatGrupalEntity) chat;
            return cg.getUsuarios() != null
                    && cg.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), usuario.getId()));
        }
        return false;
    }
}
