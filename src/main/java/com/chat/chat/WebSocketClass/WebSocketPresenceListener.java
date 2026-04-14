package com.chat.chat.WebSocketClass;

import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.PresenceService.UserPresenceService;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component
public class WebSocketPresenceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketPresenceListener.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UserPresenceService userPresenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String rawSessionId = sha == null ? null : sha.getSessionId();
        String sessionId = safeSessionId(rawSessionId);
        Long userId = resolveUserId(sha);

        if (userId == null) {
            LOGGER.warn("[WS][ESTADO] action=CONNECT result=REJECT authUserId={} destination={} reason={} sessionId={}",
                    null,
                    "-",
                    "USER_UNRESOLVED",
                    sessionId);
            return;
        }

        String destination = Constantes.TOPIC_ESTADO + userId;

        LOGGER.info("[WS][ESTADO] action=CONNECT result=ALLOW authUserId={} destination={} reason={} sessionId={}",
                userId,
                destination,
                "-",
                sessionId);

        userPresenceService.registerSessionConnected(userId, rawSessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String rawSessionId = event.getSessionId();
        String sessionId = safeSessionId(rawSessionId);
        Long userId = userPresenceService.resolveUserIdBySession(rawSessionId);

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

        userPresenceService.registerSessionDisconnected(userId, rawSessionId);
    }

    private Long resolveUserId(StompHeaderAccessor sha) {
        if (sha == null || sha.getUser() == null || sha.getUser().getName() == null || sha.getUser().getName().isBlank()) {
            return null;
        }

        String principalName = sha.getUser().getName().trim();
        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException ex) {
            return resolveUserIdByEmail(principalName);
        }
    }

    private Long resolveUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        Optional<UsuarioEntity> user = usuarioRepository.findByEmail(email);
        if (user.isPresent() && user.get().getId() != null) {
            return user.get().getId();
        }
        return null;
    }

    private String safeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "-" : sessionId;
    }
}
