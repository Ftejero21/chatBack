package com.chat.chat.Configuracion;

import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Security.JwtService;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSecurityInterceptor implements ExecutorChannelInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketSecurityInterceptor.class);
    private static final Duration ESTADO_WARN_WINDOW = Duration.ofSeconds(30);
    private static final String ESTADO_ACTION_SUBSCRIBE = "SUBSCRIBE";

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ChatGrupalRepository chatGrupalRepository;

    private final Map<String, Instant> estadoWarnDedupCache = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateProtectedSubscribe(accessor);
        }

        return message;
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && accessor.getUser() != null) {
            SecurityContextHolder.getContext().setAuthentication((Authentication) accessor.getUser());
        }
        return message;
    }

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
        SecurityContextHolder.clearContext();
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        try {
            String token = accessor.getFirstNativeHeader(Constantes.HEADER_AUTHORIZATION);
            if (token == null) {
                token = accessor.getFirstNativeHeader(Constantes.HEADER_AUTHORIZATION_LOWER);
            }

            if (token == null || !token.startsWith(Constantes.BEARER_PREFIX)) {
                return;
            }

            token = token.substring(7);
            String userEmail = jwtService.extractUsername(token);
            if (userEmail == null || userEmail.isBlank()) {
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (!jwtService.isTokenValid(token, userDetails)) {
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());
            accessor.setUser(authentication);
        } catch (Exception ex) {
            // Ruido evitado: errores esperables de JWT invalido no se elevan a ERROR.
            LOGGER.debug("[WS] action=CONNECT result=AUTH_SKIPPED sessionId={} errorClass={}",
                    accessor.getSessionId(),
                    ex.getClass().getSimpleName());
        }
    }

    private void validateProtectedSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!isProtectedDestination(destination)) {
            return;
        }

        String sessionId = accessor.getSessionId();
        Long authUserId = resolveAuthenticatedUserId(accessor);
        boolean estadoDestination = isEstadoDestination(destination);

        if (estadoDestination) {
            logEstadoInfo(ESTADO_ACTION_SUBSCRIBE, "IN", authUserId, destination, null, sessionId);
        }

        if (authUserId == null) {
            if (estadoDestination) {
                logEstadoWarnDedup(ESTADO_ACTION_SUBSCRIBE, authUserId, destination, "AUTH_REQUIRED", sessionId);
            }
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }

        if (isProtectedGroupDestination(destination)) {
            validateGroupSubscribe(destination, authUserId, sessionId, estadoDestination);
            return;
        }

        Long requestedUserId = extractProtectedUserId(destination);
        if (requestedUserId == null) {
            if (estadoDestination) {
                logEstadoWarnDedup(ESTADO_ACTION_SUBSCRIBE, authUserId, destination, "DESTINATION_INVALID", sessionId);
            }
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_INVALIDA);
        }

        if (!Objects.equals(authUserId, requestedUserId)) {
            if (estadoDestination) {
                logEstadoWarnDedup(ESTADO_ACTION_SUBSCRIBE, authUserId, destination, "USER_MISMATCH", sessionId);
            }
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }

        if (estadoDestination) {
            logEstadoInfo(ESTADO_ACTION_SUBSCRIBE, "ALLOW", authUserId, destination, null, sessionId);
        }
    }

    private void validateGroupSubscribe(String destination,
                                        Long authUserId,
                                        String sessionId,
                                        boolean estadoDestination) {
        Long requestedChatId = extractProtectedGroupChatId(destination);
        if (requestedChatId == null) {
            if (estadoDestination) {
                logEstadoWarnDedup(ESTADO_ACTION_SUBSCRIBE, authUserId, destination, "DESTINATION_INVALID", sessionId);
            }
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_INVALIDA);
        }

        Optional<ChatGrupalEntity> chatOpt = chatGrupalRepository.findByIdWithUsuarios(requestedChatId);
        if (chatOpt.isEmpty()) {
            if (estadoDestination) {
                logEstadoWarnDedup(ESTADO_ACTION_SUBSCRIBE, authUserId, destination, "CHAT_NOT_FOUND", sessionId);
            }
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }

        ChatGrupalEntity chat = chatOpt.get();
        if (!chat.isActivo()) {
            if (estadoDestination) {
                logEstadoWarnDedup(ESTADO_ACTION_SUBSCRIBE, authUserId, destination, "CHAT_INACTIVO", sessionId);
            }
            throw new AccessDeniedException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO);
        }

        boolean esMiembroActivo = chat.getUsuarios() != null && chat.getUsuarios().stream()
                .filter(Objects::nonNull)
                .anyMatch(u -> Objects.equals(u.getId(), authUserId) && u.isActivo());
        if (!esMiembroActivo) {
            if (estadoDestination) {
                logEstadoWarnDedup(ESTADO_ACTION_SUBSCRIBE, authUserId, destination, "NOT_GROUP_MEMBER", sessionId);
            }
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        if (estadoDestination) {
            logEstadoInfo(ESTADO_ACTION_SUBSCRIBE, "ALLOW", authUserId, destination, null, sessionId);
        }
    }

    private boolean isProtectedDestination(String destination) {
        return isProtectedUserDestination(destination) || isProtectedGroupDestination(destination);
    }

    private boolean isProtectedUserDestination(String destination) {
        if (destination == null || destination.isBlank()) {
            return false;
        }
        boolean chatIndividual = destination.startsWith(Constantes.TOPIC_CHAT)
                && !destination.startsWith(Constantes.TOPIC_CHAT_GRUPAL)
                && !destination.startsWith(Constantes.TOPIC_CHAT_REACCION);
        boolean chatReaccion = destination.startsWith(Constantes.TOPIC_CHAT_REACCION);
        boolean typingIndividual = destination.startsWith(Constantes.TOPIC_ESCRIBIENDO)
                && !destination.startsWith(Constantes.TOPIC_ESCRIBIENDO_GRUPO);
        boolean audioIndividual = destination.startsWith(Constantes.TOPIC_AUDIO_GRABANDO)
                && !destination.startsWith(Constantes.TOPIC_AUDIO_GRABANDO_GRUPO);
        boolean estado = destination.startsWith(Constantes.TOPIC_ESTADO);
        boolean leido = destination.startsWith(Constantes.WS_TOPIC_LEIDO);
        boolean bloqueos = isUserBloqueosDestination(destination);
        return chatIndividual || chatReaccion || typingIndividual || audioIndividual || estado || leido || bloqueos;
    }

    private boolean isProtectedGroupDestination(String destination) {
        if (destination == null || destination.isBlank()) {
            return false;
        }
        return destination.startsWith(Constantes.TOPIC_CHAT_GRUPAL)
                || destination.startsWith(Constantes.TOPIC_ESCRIBIENDO_GRUPO)
                || destination.startsWith(Constantes.TOPIC_AUDIO_GRABANDO_GRUPO);
    }

    private boolean isEstadoDestination(String destination) {
        return destination != null && destination.startsWith(Constantes.TOPIC_ESTADO);
    }

    private Long extractProtectedGroupChatId(String destination) {
        if (destination == null) {
            return null;
        }
        if (destination.startsWith(Constantes.TOPIC_CHAT_GRUPAL)) {
            return parseNumericSuffix(destination, Constantes.TOPIC_CHAT_GRUPAL);
        }
        if (destination.startsWith(Constantes.TOPIC_ESCRIBIENDO_GRUPO)) {
            return parseNumericSuffix(destination, Constantes.TOPIC_ESCRIBIENDO_GRUPO);
        }
        if (destination.startsWith(Constantes.TOPIC_AUDIO_GRABANDO_GRUPO)) {
            return parseNumericSuffix(destination, Constantes.TOPIC_AUDIO_GRABANDO_GRUPO);
        }
        return null;
    }

    private Long extractProtectedUserId(String destination) {
        if (destination == null) {
            return null;
        }
        if (destination.startsWith(Constantes.TOPIC_CHAT_REACCION)) {
            return parseNumericSuffix(destination, Constantes.TOPIC_CHAT_REACCION);
        }
        if (destination.startsWith(Constantes.TOPIC_ESTADO)) {
            return parseNumericSuffix(destination, Constantes.TOPIC_ESTADO);
        }
        if (destination.startsWith(Constantes.WS_TOPIC_LEIDO)) {
            return parseNumericSuffix(destination, Constantes.WS_TOPIC_LEIDO);
        }
        if (isUserBloqueosDestination(destination)) {
            return parseUserBloqueosUserId(destination);
        }
        if (destination.startsWith(Constantes.TOPIC_CHAT)
                && !destination.startsWith(Constantes.TOPIC_CHAT_GRUPAL)) {
            return parseChatUserSuffix(destination);
        }
        if (destination.startsWith(Constantes.TOPIC_ESCRIBIENDO)
                && !destination.startsWith(Constantes.TOPIC_ESCRIBIENDO_GRUPO)) {
            return parseNumericSuffix(destination, Constantes.TOPIC_ESCRIBIENDO);
        }
        if (destination.startsWith(Constantes.TOPIC_AUDIO_GRABANDO)
                && !destination.startsWith(Constantes.TOPIC_AUDIO_GRABANDO_GRUPO)) {
            return parseNumericSuffix(destination, Constantes.TOPIC_AUDIO_GRABANDO);
        }
        return null;
    }

    private Long parseChatUserSuffix(String destination) {
        if (destination == null || !destination.startsWith(Constantes.TOPIC_CHAT)) {
            return null;
        }
        String suffix = destination.substring(Constantes.TOPIC_CHAT.length());
        if (suffix == null || suffix.isBlank()) {
            return null;
        }

        String userCandidate = suffix;
        if (suffix.endsWith(".errors")) {
            userCandidate = suffix.substring(0, suffix.length() - ".errors".length());
        }
        if (userCandidate.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(userCandidate);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isUserBloqueosDestination(String destination) {
        return destination != null
                && destination.startsWith(Constantes.WS_TOPIC_USER_BLOQUEOS_PREFIX)
                && destination.endsWith(Constantes.WS_TOPIC_USER_BLOQUEOS_SUFFIX);
    }

    private Long parseUserBloqueosUserId(String destination) {
        if (!isUserBloqueosDestination(destination)) {
            return null;
        }
        int prefixLength = Constantes.WS_TOPIC_USER_BLOQUEOS_PREFIX.length();
        int suffixStart = destination.length() - Constantes.WS_TOPIC_USER_BLOQUEOS_SUFFIX.length();
        if (suffixStart <= prefixLength) {
            return null;
        }

        String userPart = destination.substring(prefixLength, suffixStart);
        if (userPart.isBlank() || userPart.contains("/")) {
            return null;
        }
        try {
            return Long.parseLong(userPart);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseNumericSuffix(String destination, String prefix) {
        if (destination == null || prefix == null || !destination.startsWith(prefix)) {
            return null;
        }
        String suffix = destination.substring(prefix.length());
        if (suffix.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(suffix);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long resolveAuthenticatedUserId(StompHeaderAccessor accessor) {
        if (accessor == null || accessor.getUser() == null) {
            return null;
        }
        String username = accessor.getUser().getName();
        if (username == null || username.isBlank()) {
            return null;
        }
        return usuarioRepository.findByEmail(username).map(user -> user.getId()).orElse(null);
    }

    private void logEstadoInfo(String action,
                               String result,
                               Long authUserId,
                               String destination,
                               String reason,
                               String sessionId) {
        LOGGER.info("[WS][ESTADO] action={} result={} authUserId={} destination={} reason={} sessionId={}",
                action,
                result,
                authUserId,
                destination,
                reason == null ? "-" : reason,
                sessionId);
    }

    private void logEstadoWarnDedup(String action,
                                    Long authUserId,
                                    String destination,
                                    String reason,
                                    String sessionId) {
        if (!shouldLogEstadoWarn(authUserId, destination, reason)) {
            return;
        }
        LOGGER.warn("[WS][ESTADO] action={} result=REJECT authUserId={} destination={} reason={} sessionId={}",
                action,
                authUserId,
                destination,
                reason,
                sessionId);
    }

    private boolean shouldLogEstadoWarn(Long authUserId, String destination, String reason) {
        Instant now = Instant.now();
        String key = (authUserId == null ? "null" : authUserId) + "|" + destination + "|" + reason;
        Instant prev = estadoWarnDedupCache.putIfAbsent(key, now);

        if (prev == null) {
            return true;
        }

        if (Duration.between(prev, now).compareTo(ESTADO_WARN_WINDOW) >= 0) {
            estadoWarnDedupCache.put(key, now);
            return true;
        }

        return false;
    }
}