package com.chat.chat.WebSocketClass;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketPresenceListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Map<String, String> sesionesUsuario = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String userId = sha.getFirstNativeHeader("usuarioId");

        if (userId != null) {
            sesionesUsuario.put(sha.getSessionId(), userId);
            System.out.println("✅ El usuario con ID " + userId + " se ha conectado.");
            messagingTemplate.convertAndSend("/topic/estado." + userId, "Conectado");
        } else {
            System.out.println("⚠️ Conexión sin usuarioId.");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String userId = sesionesUsuario.remove(sessionId);

        if (userId != null) {
            System.out.println("🔌 El usuario con ID " + userId + " se ha desconectado.");
            messagingTemplate.convertAndSend("/topic/estado." + userId, "Desconectado");
        } else {
            System.out.println("⚠️ Desconexión de sesión desconocida: " + sessionId);
        }
    }
}

