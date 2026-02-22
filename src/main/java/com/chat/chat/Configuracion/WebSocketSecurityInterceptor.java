package com.chat.chat.Configuracion;

import com.chat.chat.Security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.MessageHandler;
import org.springframework.security.core.Authentication;

@Component
public class WebSocketSecurityInterceptor implements ExecutorChannelInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                String token = accessor.getFirstNativeHeader("Authorization");
                if (token == null) {
                    token = accessor.getFirstNativeHeader("authorization");
                }

                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    String userEmail = jwtService.extractUsername(token);

                    if (userEmail != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                        if (jwtService.isTokenValid(token, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            accessor.setUser(authentication);
                        } else {
                            System.err.println("WebSocket CONNECT Failed: Token JWT inválido o expirado");
                        }
                    }
                } else {
                    System.err.println("WebSocket CONNECT Failed: Falta cabecera Authorization Bearer");
                }
            } catch (Exception e) {
                System.err.println("WebSocket CONNECT Exception: " + e.getMessage());
            }
        }
        return message;
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && accessor.getUser() != null) {
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication((Authentication) accessor.getUser());
        }
        return message;
    }

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
