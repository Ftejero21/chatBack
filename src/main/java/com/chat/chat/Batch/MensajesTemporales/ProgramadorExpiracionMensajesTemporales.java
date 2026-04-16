package com.chat.chat.Batch.MensajesTemporales;

import com.chat.chat.DTO.AdminDirectChatExpiredEventDTO;
import com.chat.chat.DTO.AdminDirectChatListEventDTO;
import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.Entity.ChatEntity;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.MensajeTemporalAuditoriaEntity;
import com.chat.chat.Repository.MensajeReaccionRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.MensajeTemporalAuditoriaRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.MessageType;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class ProgramadorExpiracionMensajesTemporales {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgramadorExpiracionMensajesTemporales.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MOTIVO_TEMPORAL_EXPIRADO = "TEMPORAL_EXPIRADO";

    private final MensajeRepository mensajeRepository;
    private final MensajeReaccionRepository mensajeReaccionRepository;
    private final MensajeTemporalAuditoriaRepository mensajeTemporalAuditoriaRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.mensajes.temporales.cleanup.enabled:true}")
    private boolean habilitado;

    @Value("${app.mensajes.temporales.cleanup.batch-size:500}")
    private int tamanoLote;

    @Value("${app.mensajes.temporales.cleanup.max-lotes-por-ejecucion:10}")
    private int maxLotesPorEjecucion;

    @Value("${app.mensajes.temporales.cleanup.retencion-tecnica-dias:0}")
    private int retencionTecnicaDias;

    @Value("${app.mensajes.temporales.cleanup.limpieza-tecnica.batch-size:500}")
    private int tamanoLoteLimpiezaTecnica;

    @Value(Constantes.PROP_UPLOADS_ROOT)
    private String uploadsRoot;

    public ProgramadorExpiracionMensajesTemporales(MensajeRepository mensajeRepository,
                                                   MensajeReaccionRepository mensajeReaccionRepository,
                                                   MensajeTemporalAuditoriaRepository mensajeTemporalAuditoriaRepository,
                                                   SimpMessagingTemplate messagingTemplate) {
        this.mensajeRepository = mensajeRepository;
        this.mensajeReaccionRepository = mensajeReaccionRepository;
        this.mensajeTemporalAuditoriaRepository = mensajeTemporalAuditoriaRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(
            fixedDelayString = "${app.mensajes.temporales.cleanup.fixed-delay-ms:60000}",
            initialDelayString = "${app.mensajes.temporales.cleanup.initial-delay-ms:30000}"
    )
    @Transactional
    public void expirarMensajesTemporales() {
        if (!habilitado) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        int loteSeguro = Math.max(1, tamanoLote);
        int maxLotesSeguro = Math.max(1, maxLotesPorEjecucion);
        long totalEncontrados = 0;
        long totalExpirados = 0;
        long totalErrores = 0;
        AtomicLong wsPublished = new AtomicLong();
        AtomicLong wsFailed = new AtomicLong();
        long candidatosIniciales = mensajeRepository.countMensajesTemporalesExpirados(ahora);
        LOGGER.info("[TEMP-ADMIN-EXPIRE] scheduler-start candidates={} now={}", candidatosIniciales, ahora);

        for (int i = 0; i < maxLotesSeguro; i++) {
            List<MensajeEntity> pendientes = mensajeRepository.findMensajesTemporalesPendientesExpirar(
                    ahora,
                    MOTIVO_TEMPORAL_EXPIRADO,
                    PageRequest.of(0, loteSeguro));
            if (pendientes.isEmpty()) {
                break;
            }

            totalEncontrados += pendientes.size();
            List<Long> ids = pendientes.stream()
                    .map(MensajeEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (ids.isEmpty()) {
                break;
            }

            Set<Path> adjuntosLocales = recolectarAdjuntosLocales(pendientes);
            long loteOk = 0;
            long loteFailed = 0;
            for (MensajeEntity mensaje : pendientes) {
                if (mensaje == null || mensaje.getId() == null) {
                    loteFailed++;
                    totalErrores++;
                    LOGGER.warn("[BATCH_TEMPORALES] mensaje-failed mensajeId={} chatId={} receptorId={} reason=mensaje-null",
                            null, null, null);
                    continue;
                }
                try {
                    processMensajeExpirable(mensaje, ahora, wsPublished, wsFailed);
                    loteOk++;
                    totalExpirados++;
                    LOGGER.info("[BATCH_TEMPORALES] mensaje-processed mensajeId={} chatId={} receptorId={}",
                            mensaje.getId(),
                            mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                            mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId());
                } catch (Exception ex) {
                    loteFailed++;
                    totalErrores++;
                    LOGGER.error("[BATCH_TEMPORALES] mensaje-failed mensajeId={} chatId={} receptorId={}",
                            mensaje.getId(),
                            mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                            mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                            ex);
                }
            }
            LOGGER.info("[BATCH_TEMPORALES] lote-end total={} ok={} failed={} ids={}",
                    pendientes.size(),
                    loteOk,
                    loteFailed,
                    ids);

            totalErrores += limpiarAdjuntosLocales(adjuntosLocales);
            if (pendientes.size() < loteSeguro || loteOk == 0) {
                break;
            }
        }

        int erroresLimpiezaTecnica = ejecutarLimpiezaTecnicaOpcional(ahora);
        totalErrores += erroresLimpiezaTecnica;

        if (totalEncontrados > 0 || totalErrores > 0) {
            LOGGER.info("[BATCH_TEMPORALES] encontrados={} expirados={} errores={}",
                    totalEncontrados,
                    totalExpirados,
                    totalErrores);
        }
        LOGGER.info("[TEMP-ADMIN-EXPIRE] scheduler-end expired={} wsPublished={} wsFailed={} now={}",
                totalExpirados,
                wsPublished.get(),
                wsFailed.get(),
                LocalDateTime.now());
    }

    private void processMensajeExpirable(MensajeEntity mensaje,
                                         LocalDateTime ahora,
                                         AtomicLong wsPublished,
                                         AtomicLong wsFailed) {
        String correlationId = buildCorrelationId(
                mensaje == null ? null : mensaje.getId(),
                mensaje == null || mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                mensaje == null || mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId());
        LOGGER.info("[TEMP-ADMIN-EXPIRE] step=load-start mensajeId={} chatId={} receptorId={} correlationId={}",
                mensaje == null ? null : mensaje.getId(),
                mensaje == null || mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                mensaje == null || mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                correlationId);

        logDeteccionExpiracion(mensaje, ahora);

        LOGGER.info("[TEMP-ADMIN-EXPIRE] step=mark-expired-start mensajeId={} chatId={} receptorId={} correlationId={}",
                mensaje.getId(),
                mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                correlationId);
        mensajeReaccionRepository.deleteByMensajeIdIn(List.of(mensaje.getId()));
        guardarAuditoriaOriginalSiNoExiste(mensaje);
        aplicarPlaceholderExpirado(mensaje);
        MensajeEntity persisted = mensajeRepository.save(mensaje);
        LOGGER.info("[TEMP-ADMIN-EXPIRE] step=mark-expired-ok mensajeId={} chatId={} receptorId={} correlationId={}",
                persisted.getId(),
                persisted.getChat() == null ? null : persisted.getChat().getId(),
                persisted.getReceptor() == null ? null : persisted.getReceptor().getId(),
                correlationId);

        LOGGER.info("[TEMP-ADMIN-EXPIRE] step=build-ws-events-start mensajeId={} chatId={} receptorId={} correlationId={}",
                persisted.getId(),
                persisted.getChat() == null ? null : persisted.getChat().getId(),
                persisted.getReceptor() == null ? null : persisted.getReceptor().getId(),
                correlationId);
        LOGGER.info("[TEMP-ADMIN-EXPIRE] step=build-ws-events-ok mensajeId={} chatId={} receptorId={} correlationId={}",
                persisted.getId(),
                persisted.getChat() == null ? null : persisted.getChat().getId(),
                persisted.getReceptor() == null ? null : persisted.getReceptor().getId(),
                correlationId);

        emitirActualizacionEnTiempoReal(persisted, wsPublished, wsFailed);
    }

    private void aplicarPlaceholderExpirado(MensajeEntity mensaje) {
        if (mensaje == null) {
            LOGGER.warn("[TEMP-ADMIN-WS] skip correlationId={} step=mark-expired reason=mensaje-null",
                    buildCorrelationId(null, null, null));
            return;
        }
        String placeholder = mensaje.getPlaceholderTexto();
        if (placeholder == null || placeholder.isBlank()) {
            placeholder = Utils.construirPlaceholderTemporal(mensaje.getMensajeTemporalSegundos());
        }
        mensaje.setActivo(false);
        mensaje.setFechaEliminacion(LocalDateTime.now(java.time.ZoneOffset.UTC));
        mensaje.setLeido(true);
        mensaje.setTipo(com.chat.chat.Utils.MessageType.TEXT);
        mensaje.setContenido(placeholder);
        mensaje.setPlaceholderTexto(placeholder);
        mensaje.setMotivoEliminacion(MOTIVO_TEMPORAL_EXPIRADO);
        mensaje.setExpiredByPolicy(true);
        mensaje.setExpireAt(mensaje.getExpiraEn());
        mensaje.setMediaUrl(null);
        mensaje.setMediaMime(null);
        mensaje.setMediaDuracionMs(null);
        mensaje.setMediaSizeBytes(null);
    }

    private void emitirActualizacionEnTiempoReal(MensajeEntity mensaje, AtomicLong wsPublished, AtomicLong wsFailed) {
        if (mensaje == null || mensaje.getId() == null) {
            LOGGER.warn("[TEMP-ADMIN-WS] skip type=UNKNOWN chatId={} userId={} reason=mensaje-null",
                    mensaje == null || mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                    null);
            return;
        }
        String correlationId = buildCorrelationId(
                mensaje.getId(),
                mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId());
        try {
            LOGGER.info("[TEMP-ADMIN-EXPIRE] step=publish-ws-start mensajeId={} chatId={} receptorId={} correlationId={}",
                    mensaje.getId(),
                    mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                    mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                    correlationId);
            ChatEntity chat = mensaje.getChat();
            if (chat == null) {
                LOGGER.warn("[TEMP-ADMIN-WS] skip correlationId={} step=publish-ws reason=chat-null", correlationId);
                return;
            }
            boolean groupChat = chat instanceof ChatGrupalEntity;
            boolean individualLikeChat = !groupChat;
            LOGGER.info("[TEMP-ADMIN-EXPIRE] step=route-ws mensajeId={} chatId={} receptorId={} correlationId={} adminMessage={} chatClass={} groupChat={} individualLikeChat={}",
                    mensaje.getId(),
                    chat.getId(),
                    mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                    correlationId,
                    mensaje.isAdminMessage(),
                    chat.getClass().getName(),
                    groupChat,
                    individualLikeChat);
            if (mensaje.isAdminMessage() && individualLikeChat) {
                Long receptorId = mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId();
                if (receptorId == null) {
                    LOGGER.warn("[TEMP-ADMIN-WS] skip type=TEMPORAL_MESSAGE_EXPIRED chatId={} userId={} correlationId={} reason=no-user-id",
                            chat == null ? null : chat.getId(),
                            null,
                            correlationId);
                    return;
                }
                if (receptorId != null) {
                    MensajeDTO dto = MappingUtils.mensajeEntityADto(mensaje);
                    if (dto == null) {
                        LOGGER.warn("[TEMP-ADMIN-WS] skip correlationId={} step=build-timeline-event reason=mensaje-dto-null", correlationId);
                        return;
                    }
                    dto.setSystemEvent("TEMPORAL_MESSAGE_EXPIRED");
                    publishWs(
                            "TEMPORAL_MESSAGE_EXPIRED",
                            Constantes.TOPIC_CHAT + receptorId,
                            dto,
                            correlationId,
                            mensaje.getId(),
                            chat.getId(),
                            receptorId,
                            wsPublished,
                            wsFailed);

                    AdminDirectChatExpiredEventDTO event = new AdminDirectChatExpiredEventDTO();
                    event.setSystemEvent(Constantes.SYSTEM_EVENT_ADMIN_DIRECT_CHAT_EXPIRED);
                    event.setChatId(chat.getId());
                    event.setUserId(receptorId);
                    event.setExpiredMessageIds(List.of(mensaje.getId()));
                    publishWs(
                            Constantes.SYSTEM_EVENT_ADMIN_DIRECT_CHAT_EXPIRED,
                            Constantes.TOPIC_CHAT + receptorId,
                            event,
                            correlationId,
                            mensaje.getId(),
                            chat.getId(),
                            receptorId,
                            wsPublished,
                            wsFailed);

                    emitirEstadoListaChatAdmin(chat.getId(), receptorId, correlationId, wsPublished, wsFailed);
                }
                LOGGER.info("[TEMP-ADMIN-EXPIRE] step=publish-ws-ok mensajeId={} chatId={} receptorId={} correlationId={}",
                        mensaje.getId(),
                        chat.getId(),
                        receptorId,
                        correlationId);
                return;
            }

            if (!mensaje.isAdminMessage()) {
                LOGGER.info("[TEMP-ADMIN-WS] skip type=TEMPORAL_MESSAGE_EXPIRED chatId={} userId={} correlationId={} reason=no-era-admin-temporal",
                        chat == null ? null : chat.getId(),
                        mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                        correlationId);
            }

            var dto = MappingUtils.mensajeEntityADto(mensaje);
            if (dto == null) {
                LOGGER.warn("[TEMP-ADMIN-WS] skip correlationId={} step=build-generic-event reason=mensaje-dto-null", correlationId);
                return;
            }
            dto.setSystemEvent("TEMPORAL_MESSAGE_EXPIRED");
            if (chat instanceof ChatGrupalEntity) {
                if (chat.getId() != null) {
                    publishWs(
                            "TEMPORAL_MESSAGE_EXPIRED",
                            Constantes.TOPIC_CHAT_GRUPAL + chat.getId(),
                            dto,
                            correlationId,
                            mensaje.getId(),
                            chat.getId(),
                            null,
                            wsPublished,
                            wsFailed);
                }
                LOGGER.info("[TEMP-ADMIN-EXPIRE] step=publish-ws-ok mensajeId={} chatId={} receptorId={} correlationId={}",
                        mensaje.getId(),
                        chat.getId(),
                        mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                        correlationId);
                return;
            }

            Long emisorId = mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId();
            Long receptorId = mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId();
            if (emisorId != null) {
                publishWs(
                        "TEMPORAL_MESSAGE_EXPIRED",
                        Constantes.TOPIC_CHAT + emisorId,
                        dto,
                        correlationId,
                        mensaje.getId(),
                        chat == null ? null : chat.getId(),
                        emisorId,
                        wsPublished,
                        wsFailed);
            } else {
                LOGGER.warn("[TEMP-ADMIN-WS] skip type=TEMPORAL_MESSAGE_EXPIRED chatId={} userId={} correlationId={} reason=no-emisor-id",
                        chat == null ? null : chat.getId(),
                        null,
                        correlationId);
            }
            if (receptorId != null) {
                publishWs(
                        "TEMPORAL_MESSAGE_EXPIRED",
                        Constantes.TOPIC_CHAT + receptorId,
                        dto,
                        correlationId,
                        mensaje.getId(),
                        chat == null ? null : chat.getId(),
                        receptorId,
                        wsPublished,
                        wsFailed);
            } else {
                LOGGER.warn("[TEMP-ADMIN-WS] skip type=TEMPORAL_MESSAGE_EXPIRED chatId={} userId={} correlationId={} reason=no-receptor-id",
                        chat == null ? null : chat.getId(),
                        null,
                        correlationId);
            }
            LOGGER.info("[TEMP-ADMIN-EXPIRE] step=publish-ws-ok mensajeId={} chatId={} receptorId={} correlationId={}",
                    mensaje.getId(),
                    chat.getId(),
                    receptorId,
                    correlationId);
        } catch (Exception ex) {
            wsFailed.incrementAndGet();
            LOGGER.error("[TEMP-ADMIN-WS] error type=TEMPORAL_MESSAGE_EXPIRED chatId={} userId={} correlationId={} ex={} message={}",
                    mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                    mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                    correlationId,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex);
        }
    }

    private void emitirEstadoListaChatAdmin(Long chatId,
                                            Long userId,
                                            String correlationId,
                                            AtomicLong wsPublished,
                                            AtomicLong wsFailed) {
        if (chatId == null || userId == null) {
            LOGGER.warn("[TEMP-ADMIN-WS] skip type=ADMIN_DIRECT_CHAT_LIST_UPDATED chatId={} userId={} correlationId={} reason={}",
                    chatId,
                    userId,
                    correlationId,
                    chatId == null ? "chat-null" : "no-user-id");
            return;
        }
        try {
            MensajeEntity lastVisible = mensajeRepository
                    .findTopVisibleByChatIdOrderByFechaEnvioDesc(chatId, null)
                    .orElse(null);
            LOGGER.info("[TEMP-ADMIN-EXPIRE] step=build-ws-events-start mensajeId={} chatId={} receptorId={} correlationId={}",
                    lastVisible == null ? null : lastVisible.getId(),
                    chatId,
                    userId,
                    correlationId);

            AdminDirectChatListEventDTO event = new AdminDirectChatListEventDTO();
            event.setChatId(chatId);
            event.setUserId(userId);

            if (lastVisible == null) {
                LOGGER.info("[TEMP-ADMIN-EXPIRE] sidebar chatId={} userId={} correlationId={} removed=true reason=no_visible_messages_left",
                        chatId,
                        userId,
                        correlationId);
                event.setSystemEvent(Constantes.SYSTEM_EVENT_ADMIN_DIRECT_CHAT_REMOVED);
                event.setRemoved(true);
                publishWs(
                        Constantes.SYSTEM_EVENT_ADMIN_DIRECT_CHAT_REMOVED,
                        Constantes.TOPIC_CHAT + userId,
                        event,
                        correlationId,
                        null,
                        chatId,
                        userId,
                        wsPublished,
                        wsFailed);
                return;
            }

            String preview = buildIndividualPreview(lastVisible, userId);
            LOGGER.info("[TEMP-ADMIN-EXPIRE] step=build-ws-events-ok mensajeId={} chatId={} receptorId={} correlationId={}",
                    lastVisible.getId(),
                    chatId,
                    userId,
                    correlationId);
            LOGGER.info("[TEMP-ADMIN-EXPIRE] sidebar chatId={} userId={} correlationId={} removed=false lastVisibleMessageId={} lastVisibleMessageTipo={} lastVisibleMessagePreview={}",
                    chatId,
                    userId,
                    correlationId,
                    lastVisible.getId(),
                    resolvePreviewType(lastVisible),
                    preview);
            event.setSystemEvent(Constantes.SYSTEM_EVENT_ADMIN_DIRECT_CHAT_LIST_UPDATED);
            event.setRemoved(false);
            event.setLastVisibleMessageId(lastVisible.getId());
            event.setUltimoMensaje(preview);
            event.setUltimoMensajeTipo(resolvePreviewType(lastVisible));
            event.setUltimoMensajeEmisorId(lastVisible.getEmisor() == null ? null : lastVisible.getEmisor().getId());
            event.setUltimaFecha(lastVisible.getFechaEnvio());
            publishWs(
                    Constantes.SYSTEM_EVENT_ADMIN_DIRECT_CHAT_LIST_UPDATED,
                    Constantes.TOPIC_CHAT + userId,
                    event,
                    correlationId,
                    lastVisible.getId(),
                    chatId,
                    userId,
                    wsPublished,
                    wsFailed);

        } catch (Exception ex) {
            wsFailed.incrementAndGet();
            LOGGER.error("[TEMP-ADMIN-WS] error type=ADMIN_DIRECT_CHAT_LIST_UPDATED chatId={} userId={} correlationId={} ex={} message={}",
                    chatId,
                    userId,
                    correlationId,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex);
        }
    }

    private void logDeteccionExpiracion(MensajeEntity mensaje, LocalDateTime now) {
        if (mensaje == null) {
            LOGGER.warn("[TEMP-ADMIN-EXPIRE] detected mensajeId={} chatId={} receptorId={} emisorId={} adminMessage={} mensajeTemporal={} estadoAntes={} estadoDespues={} motivo={} expireAt={} now={} firstReadAt={} reason=mensaje-null",
                    null, null, null, null, null, null, null, "EXPIRADO", null, null, now, null);
            return;
        }
        String estadoAntes = mensaje.getExpiraEn() != null && mensaje.getExpiraEn().isAfter(now) ? "ACTIVO" : "PENDIENTE_EXPIRACION";
        LOGGER.info("[TEMP-ADMIN-EXPIRE] detected mensajeId={} chatId={} receptorId={} emisorId={} adminMessage={} mensajeTemporal={} estadoAntes={} estadoDespues=EXPIRADO motivo={} expireAt={} now={} firstReadAt={}",
                mensaje.getId(),
                mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId(),
                mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId(),
                mensaje.isAdminMessage(),
                mensaje.isMensajeTemporal(),
                estadoAntes,
                MOTIVO_TEMPORAL_EXPIRADO,
                mensaje.getExpireAt(),
                now,
                mensaje.getFirstReadAt());
    }

    private String buildCorrelationId(Long mensajeId, Long chatId, Long userId) {
        return "tempAdmin:" + mensajeId + ":" + chatId + ":" + userId;
    }

    private void publishWs(String type,
                           String topic,
                           Object payload,
                           String correlationId,
                           Long mensajeId,
                           Long chatId,
                           Long userId,
                           AtomicLong wsPublished,
                           AtomicLong wsFailed) {
        if (topic == null || topic.isBlank()) {
            LOGGER.warn("[TEMP-ADMIN-WS] skip type={} chatId={} userId={} correlationId={} reason=no-topic",
                    type,
                    chatId,
                    userId,
                    correlationId);
            return;
        }
        if (userId == null && !topic.startsWith(Constantes.TOPIC_CHAT_GRUPAL)) {
            LOGGER.warn("[TEMP-ADMIN-WS] skip type={} chatId={} userId={} correlationId={} reason=no-user-id",
                    type,
                    chatId,
                    null,
                    correlationId);
            return;
        }
        try {
            LOGGER.info("[TEMP-ADMIN-WS] publish type={} topic={} mensajeId={} chatId={} userId={} correlationId={} payloadClass={} payload={}",
                    type,
                    topic,
                    mensajeId,
                    chatId,
                    userId,
                    correlationId,
                    payload == null ? null : payload.getClass().getSimpleName(),
                    summarizePayload(payload));
            messagingTemplate.convertAndSend(topic, payload);
            wsPublished.incrementAndGet();
        } catch (Exception ex) {
            wsFailed.incrementAndGet();
            LOGGER.error("[TEMP-ADMIN-WS] error type={} chatId={} userId={} correlationId={} ex={} message={}",
                    type,
                    chatId,
                    userId,
                    correlationId,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex);
        }
    }

    private String summarizePayload(Object payload) {
        if (payload == null) {
            return "null";
        }
        if (payload instanceof MensajeDTO dto) {
            return "MensajeDTO{id=" + dto.getId()
                    + ",chatId=" + dto.getChatId()
                    + ",emisorId=" + dto.getEmisorId()
                    + ",receptorId=" + dto.getReceptorId()
                    + ",activo=" + dto.isActivo()
                    + ",adminMessage=" + dto.getAdminMessage()
                    + ",mensajeTemporal=" + dto.getMensajeTemporal()
                    + ",estadoTemporal=" + dto.getEstadoTemporal()
                    + ",motivoEliminacion=" + dto.getMotivoEliminacion()
                    + ",expiredByPolicy=" + dto.getExpiredByPolicy()
                    + ",systemEvent=" + dto.getSystemEvent()
                    + "}";
        }
        if (payload instanceof AdminDirectChatExpiredEventDTO dto) {
            return "AdminDirectChatExpiredEventDTO{chatId=" + dto.getChatId()
                    + ",userId=" + dto.getUserId()
                    + ",expiredMessageIds=" + dto.getExpiredMessageIds()
                    + ",systemEvent=" + dto.getSystemEvent()
                    + "}";
        }
        if (payload instanceof AdminDirectChatListEventDTO dto) {
            return "AdminDirectChatListEventDTO{chatId=" + dto.getChatId()
                    + ",userId=" + dto.getUserId()
                    + ",removed=" + dto.isRemoved()
                    + ",lastVisibleMessageId=" + dto.getLastVisibleMessageId()
                    + ",ultimoMensajeTipo=" + dto.getUltimoMensajeTipo()
                    + ",systemEvent=" + dto.getSystemEvent()
                    + "}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            return "serialization_error:" + ex.getClass().getSimpleName();
        }
    }

    private String resolvePreviewType(MensajeEntity mensaje) {
        MessageType tipo = mensaje == null || mensaje.getTipo() == null ? MessageType.TEXT : mensaje.getTipo();
        return tipo.name();
    }

    private String buildIndividualPreview(MensajeEntity mensaje, Long viewerId) {
        if (mensaje == null) {
            LOGGER.warn("[TEMP-ADMIN-WS] skip correlationId={} step=preview-builder reason=last-visible-null",
                    buildCorrelationId(null, null, viewerId));
            return null;
        }
        if (mensaje.getTipo() == null) {
            LOGGER.warn("[TEMP-ADMIN-WS] skip correlationId={} step=preview-builder reason=tipo-null",
                    buildCorrelationId(mensaje.getId(), mensaje.getChat() == null ? null : mensaje.getChat().getId(), viewerId));
        }
        boolean own = mensaje.getEmisor() != null && Objects.equals(mensaje.getEmisor().getId(), viewerId);
        String prefix = own ? "Tu: " : "";
        MessageType tipo = mensaje.getTipo() == null ? MessageType.TEXT : mensaje.getTipo();
        if (tipo == MessageType.IMAGE) {
            return prefix + "Imagen";
        }
        if (tipo == MessageType.AUDIO) {
            String dur = Utils.mmss(mensaje.getMediaDuracionMs());
            return prefix + "Audio" + ((dur == null || dur.isBlank()) ? "" : " (" + dur + ")");
        }
        if (tipo == MessageType.FILE) {
            String mediaUrl = mensaje.getMediaUrl();
            String fileName = mediaUrl == null ? "Archivo" : mediaUrl.substring(mediaUrl.lastIndexOf('/') + 1);
            return prefix + "Archivo: " + fileName;
        }
        if (tipo == MessageType.POLL) {
            return prefix + "Encuesta";
        }
        return prefix + Utils.truncarSafe(mensaje.getContenido(), 60);
    }

    private void guardarAuditoriaOriginalSiNoExiste(MensajeEntity mensaje) {
        if (mensaje == null || mensaje.getId() == null) {
            return;
        }
        if (mensajeTemporalAuditoriaRepository.findByMensajeId(mensaje.getId()).isPresent()) {
            return;
        }

        MensajeTemporalAuditoriaEntity auditoria = new MensajeTemporalAuditoriaEntity();
        auditoria.setMensajeId(mensaje.getId());
        auditoria.setChatId(mensaje.getChat() == null ? null : mensaje.getChat().getId());
        auditoria.setContenidoOriginal(mensaje.getContenido());
        auditoria.setTipoOriginal(mensaje.getTipo() == null ? null : mensaje.getTipo().name());
        auditoria.setMediaUrlOriginal(mensaje.getMediaUrl());
        auditoria.setMediaMimeOriginal(mensaje.getMediaMime());
        auditoria.setMediaDuracionMsOriginal(mensaje.getMediaDuracionMs());
        auditoria.setReenviado(mensaje.isReenviado());
        auditoria.setMensajeOriginalId(mensaje.getMensajeOriginalId());
        auditoria.setReplyToMessageId(mensaje.getReplyToMessageId());
        auditoria.setReplySnippet(mensaje.getReplySnippet());
        auditoria.setReplyAuthorName(mensaje.getReplyAuthorName());
        auditoria.setFechaEnvioOriginal(mensaje.getFechaEnvio());
        auditoria.setExpiraEnOriginal(mensaje.getExpiraEn());
        auditoria.setEstadoTemporalOriginal("EXPIRADO");

        UrlsAuditoria urls = extraerUrlsAuditoria(mensaje);
        auditoria.setAudioUrlOriginal(urls.audioUrl());
        auditoria.setImageUrlOriginal(urls.imageUrl());
        auditoria.setFileUrlOriginal(urls.fileUrl());

        mensajeTemporalAuditoriaRepository.save(auditoria);
    }

    private UrlsAuditoria extraerUrlsAuditoria(MensajeEntity mensaje) {
        String audioUrl = null;
        String imageUrl = null;
        String fileUrl = null;
        if (mensaje != null && mensaje.getTipo() != null) {
            switch (mensaje.getTipo()) {
                case AUDIO -> audioUrl = mensaje.getMediaUrl();
                case IMAGE -> imageUrl = mensaje.getMediaUrl();
                case FILE -> fileUrl = mensaje.getMediaUrl();
                default -> {
                    // no-op
                }
            }
        }
        String contenido = mensaje == null ? null : mensaje.getContenido();
        if (contenido != null && !contenido.isBlank()) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(contenido);
                if (audioUrl == null || audioUrl.isBlank()) {
                    audioUrl = root.path("audioUrl").asText(null);
                }
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = root.path("imageUrl").asText(null);
                }
                if (fileUrl == null || fileUrl.isBlank()) {
                    fileUrl = root.path("fileUrl").asText(null);
                    if (fileUrl == null || fileUrl.isBlank()) {
                        fileUrl = root.path("url").asText(null);
                    }
                }
            } catch (Exception ignored) {
                // contenido no JSON: usamos solo mediaUrl principal.
            }
        }
        return new UrlsAuditoria(audioUrl, imageUrl, fileUrl);
    }

    private int ejecutarLimpiezaTecnicaOpcional(LocalDateTime ahora) {
        if (retencionTecnicaDias <= 0) {
            return 0;
        }
        int errores = 0;
        LocalDateTime cutoff = ahora.minusDays(retencionTecnicaDias);
        int lote = Math.max(1, tamanoLoteLimpiezaTecnica);
        List<MensajeEntity> candidatos = mensajeRepository.findPlaceholdersParaLimpiezaTecnica(
                MOTIVO_TEMPORAL_EXPIRADO,
                cutoff,
                PageRequest.of(0, lote));
        if (candidatos.isEmpty()) {
            return 0;
        }
        List<Long> ids = candidatos.stream()
                .map(MensajeEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return 0;
        }
        try {
            mensajeRepository.eliminarPorIds(ids);
        } catch (Exception ex) {
            errores += ids.size();
            LOGGER.warn("[BATCH_TEMPORALES] Error en limpieza tecnica de placeholders ids={} error={}",
                    ids,
                    ex.getClass().getSimpleName());
        }
        return errores;
    }

    private Set<Path> recolectarAdjuntosLocales(List<MensajeEntity> mensajes) {
        Set<Path> rutas = new LinkedHashSet<>();
        for (MensajeEntity mensaje : mensajes) {
            if (mensaje == null) {
                continue;
            }
            agregarRutaLocalSiCorresponde(mensaje.getMediaUrl(), rutas);
            agregarRutasDesdeContenido(mensaje.getContenido(), rutas);
        }
        return rutas;
    }

    private void agregarRutasDesdeContenido(String contenido, Set<Path> rutas) {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(contenido);
            agregarRutaLocalSiCorresponde(root.path("audioUrl").asText(null), rutas);
            agregarRutaLocalSiCorresponde(root.path("imageUrl").asText(null), rutas);
            agregarRutaLocalSiCorresponde(root.path("mediaUrl").asText(null), rutas);
            agregarRutaLocalSiCorresponde(root.path("fileUrl").asText(null), rutas);
            agregarRutaLocalSiCorresponde(root.path("url").asText(null), rutas);
        } catch (Exception ignored) {
            // Si no es JSON, no hay rutas adicionales que extraer.
        }
    }

    private void agregarRutaLocalSiCorresponde(String urlPublica, Set<Path> rutas) {
        if (urlPublica == null || urlPublica.isBlank() || !urlPublica.startsWith(Constantes.UPLOADS_PREFIX)) {
            return;
        }

        String relativa = urlPublica.substring(Constantes.UPLOADS_PREFIX.length());
        Path raiz = Paths.get(uploadsRoot).toAbsolutePath().normalize();
        Path archivo = raiz.resolve(relativa).normalize();
        if (!archivo.startsWith(raiz)) {
            return;
        }
        rutas.add(archivo);
    }

    private int limpiarAdjuntosLocales(Set<Path> rutas) {
        int errores = 0;
        for (Path ruta : rutas) {
            try {
                Files.deleteIfExists(ruta);
            } catch (Exception ex) {
                errores++;
                LOGGER.warn("[BATCH_TEMPORALES] No se pudo limpiar adjunto local {}: {}", ruta, ex.getClass().getSimpleName());
            }
        }
        return errores;
    }

    private record UrlsAuditoria(String audioUrl, String imageUrl, String fileUrl) {
    }
}
