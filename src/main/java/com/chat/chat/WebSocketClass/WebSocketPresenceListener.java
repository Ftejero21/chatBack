package com.chat.chat.WebSocketClass;

import com.chat.chat.Configuracion.EstadoUsuarioManager;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.PresenceService.PresenceBroadcastService;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketPresenceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketPresenceListener.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstadoUsuarioManager estadoUsuarioManager;

    @Autowired
    private PresenceBroadcastService presenceBroadcastService;

    private static final Map<String, Long> sesionesUsuario = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = safeSessionId(sha == null ? null : sha.getSessionId());
        Long userId = resolveUserId(sha);

        if (userId == null) {
            LOGGER.warn("[WS][ESTADO] action=CONNECT result=REJECT authUserId={} destination={} reason={} sessionId={}",
                    null,
                    "-",
                    "USER_UNRESOLVED",
                    sessionId);
            return;
        }

        sesionesUsuario.put(sessionId, userId);
        String destination = Constantes.TOPIC_ESTADO + userId;

        LOGGER.info("[WS][ESTADO] action=CONNECT result=ALLOW authUserId={} destination={} reason={} sessionId={}",
                userId,
                destination,
                "-",
                sessionId);

        estadoUsuarioManager.marcarConectado(userId);
        presenceBroadcastService.publishPresenceToAuthorized(userId, Constantes.ESTADO_CONECTADO, sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = safeSessionId(event.getSessionId());
        Long userId = sesionesUsuario.remove(sessionId);

        if (userId == null) {
            StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
            userId = resolveUserId(sha);
        }

        if (userId == null) {
            LOGGER.warn("[WS][ESTADO] action=DISCONNECT result=REJECT authUserId={} destination={} reason={} sessionId={}",
                    null,
                    "-",
                    "SESSION_UNKNOWN",
                    sessionId);
            return;
        }

        String destination = Constantes.TOPIC_ESTADO + userId;

        LOGGER.info("[WS][ESTADO] action=DISCONNECT result=ALLOW authUserId={} destination={} reason={} sessionId={}",
                userId,
                destination,
                "-",
                sessionId);

        estadoUsuarioManager.marcarDesconectado(userId);
        presenceBroadcastService.publishPresenceToAuthorized(userId, Constantes.ESTADO_DESCONECTADO, sessionId);
    }

    private Long resolveUserId(StompHeaderAccessor sha) {
        if (sha == null) {
            return null;
        }

        if (sha.getUser() != null && sha.getUser().getName() != null && !sha.getUser().getName().isBlank()) {
            Optional<UsuarioEntity> user = usuarioRepository.findByEmail(sha.getUser().getName());
            if (user.isPresent() && user.get().getId() != null) {
                return user.get().getId();
            }
        }

        String userIdHeader = sha.getFirstNativeHeader(Constantes.HEADER_USUARIO_ID);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "-" : sessionId;
    }
}
