package com.chat.chat.Service.PresenceService;

import com.chat.chat.Configuracion.EstadoUsuarioManager;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class UserPresenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPresenceService.class);

    @Autowired
    private PresenceBroadcastService presenceBroadcastService;

    @Autowired
    private EstadoUsuarioManager estadoUsuarioManager;

    @Value("${app.presence.away-timeout-ms:300000}")
    private long awayTimeoutMs;

    private final ConcurrentMap<Long, UserPresenceAggregate> aggregateByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> userBySession = new ConcurrentHashMap<>();

    public void registerSessionConnected(Long userId, String sessionId) {
        if (userId == null) {
            return;
        }
        Instant now = Instant.now();
        String resolvedSessionId = normalizeSessionId(sessionId, userId);
        bindSessionToUser(resolvedSessionId, userId, now);
        updateAggregate(userId, aggregate -> {
            SessionPresence session = aggregate.sessions.computeIfAbsent(resolvedSessionId, ignored -> new SessionPresence());
            session.visible = true;
            session.lastSeenAt = maxInstant(session.lastSeenAt, now);
        }, now, "CONNECT");
    }

    public void registerSessionDisconnected(Long fallbackUserId, String sessionId) {
        Instant now = Instant.now();
        String resolvedSessionId = sanitizeSessionId(sessionId);
        Long mappedUserId = resolvedSessionId == null ? null : userBySession.remove(resolvedSessionId);
        Long resolvedUserId = mappedUserId != null ? mappedUserId : fallbackUserId;
        if (resolvedUserId == null) {
            return;
        }

        updateAggregate(resolvedUserId, aggregate -> {
            if (resolvedSessionId == null) {
                aggregate.sessions.clear();
            } else {
                aggregate.sessions.remove(resolvedSessionId);
            }
        }, now, "DISCONNECT");
    }

    public void handlePresenceSignal(Long userId, String sessionId, String requestedState) {
        if (userId == null) {
            return;
        }
        Instant now = Instant.now();
        String resolvedSessionId = normalizeSessionId(sessionId, userId);
        String normalizedState = presenceBroadcastService.normalizeEstado(requestedState);
        bindSessionToUser(resolvedSessionId, userId, now);

        updateAggregate(userId, aggregate -> {
            SessionPresence session = aggregate.sessions.computeIfAbsent(resolvedSessionId, ignored -> new SessionPresence());
            if (Constantes.ESTADO_CONECTADO.equalsIgnoreCase(normalizedState)) {
                session.visible = true;
                session.lastSeenAt = maxInstant(session.lastSeenAt, now);
                return;
            }
            session.visible = false;
            session.lastVisibilityAt = maxInstant(session.lastVisibilityAt, now);
        }, now, "SIGNAL_" + normalizedState);
    }

    public Long resolveUserIdBySession(String sessionId) {
        String normalized = sanitizeSessionId(sessionId);
        if (normalized == null) {
            return null;
        }
        return userBySession.get(normalized);
    }

    @Scheduled(
            fixedDelayString = "${app.presence.reconcile.fixed-delay-ms:45000}",
            initialDelayString = "${app.presence.reconcile.initial-delay-ms:15000}"
    )
    public void reconcilePresenceStates() {
        Instant now = Instant.now();
        for (Long userId : new ArrayList<>(aggregateByUser.keySet())) {
            updateAggregate(userId, aggregate -> {
            }, now, "SCHEDULER");
        }
    }

    private void bindSessionToUser(String sessionId, Long userId, Instant now) {
        Long previousUser = userBySession.put(sessionId, userId);
        if (previousUser != null && !Objects.equals(previousUser, userId)) {
            updateAggregate(previousUser, aggregate -> aggregate.sessions.remove(sessionId), now, "SESSION_REBIND");
        }
    }

    private void updateAggregate(Long userId,
                                 Consumer<UserPresenceAggregate> mutator,
                                 Instant now,
                                 String reason) {
        if (userId == null) {
            return;
        }

        AtomicReference<PresenceTransition> transitionRef = new AtomicReference<>();

        aggregateByUser.compute(userId, (id, current) -> {
            UserPresenceAggregate aggregate = current == null ? new UserPresenceAggregate() : current;
            String previousState = aggregate.currentState;

            mutator.accept(aggregate);

            AggregateSnapshot snapshot = calculateSnapshot(aggregate, now);
            aggregate.lastSeenAt = snapshot.lastSeenAt;
            aggregate.lastVisibilityAt = snapshot.lastVisibilityAt;
            aggregate.currentState = snapshot.currentState;

            if (!Objects.equals(previousState, snapshot.currentState)) {
                transitionRef.set(new PresenceTransition(id, previousState, snapshot.currentState, reason));
            }

            if (aggregate.sessions.isEmpty() && Constantes.ESTADO_DESCONECTADO.equals(snapshot.currentState)) {
                return null;
            }
            return aggregate;
        });

        PresenceTransition transition = transitionRef.get();
        if (transition != null) {
            applyTransition(transition);
        }
    }

    private AggregateSnapshot calculateSnapshot(UserPresenceAggregate aggregate, Instant now) {
        if (aggregate.sessions.isEmpty()) {
            return new AggregateSnapshot(
                    Constantes.ESTADO_DESCONECTADO,
                    aggregate.lastSeenAt,
                    aggregate.lastVisibilityAt);
        }

        boolean hasVisibleSession = false;
        Instant latestSeenAt = null;
        Instant latestVisibilityAt = null;

        for (SessionPresence session : aggregate.sessions.values()) {
            if (session == null) {
                continue;
            }
            latestSeenAt = maxInstant(latestSeenAt, session.lastSeenAt);
            if (session.visible) {
                hasVisibleSession = true;
            } else {
                latestVisibilityAt = maxInstant(latestVisibilityAt, session.lastVisibilityAt);
            }
        }

        String state = Constantes.ESTADO_CONECTADO;
        if (!hasVisibleSession) {
            Instant inactivityReference = latestSeenAt != null ? latestSeenAt : latestVisibilityAt;
            boolean thresholdExceeded = inactivityReference == null
                    || Duration.between(inactivityReference, now).toMillis() >= awayTimeoutMs;
            if (thresholdExceeded) {
                state = Constantes.ESTADO_AUSENTE;
            }
        }

        return new AggregateSnapshot(state, latestSeenAt, latestVisibilityAt);
    }

    private void applyTransition(PresenceTransition transition) {
        if (transition == null || transition.userId == null) {
            return;
        }
        estadoUsuarioManager.actualizarEstado(transition.userId, transition.currentState);
        presenceBroadcastService.publishPresenceToAuthorized(transition.userId, transition.currentState, "-");
        LOGGER.info("[WS][ESTADO] action=TRANSITION result=OK authUserId={} destination={} reason={} sessionId={}",
                transition.userId,
                Constantes.TOPIC_ESTADO + transition.userId,
                transition.previousState + "->" + transition.currentState + " (" + transition.reason + ")",
                "-");
    }

    private String normalizeSessionId(String sessionId, Long userId) {
        String normalized = sanitizeSessionId(sessionId);
        if (normalized == null) {
            return "user-" + userId;
        }
        return normalized;
    }

    private String sanitizeSessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        String normalized = sessionId.trim();
        if (normalized.isEmpty() || "-".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private Instant maxInstant(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private static class UserPresenceAggregate {
        private final Map<String, SessionPresence> sessions = new HashMap<>();
        private Instant lastSeenAt;
        private Instant lastVisibilityAt;
        private String currentState = Constantes.ESTADO_DESCONECTADO;
    }

    private static class SessionPresence {
        private Instant lastSeenAt;
        private Instant lastVisibilityAt;
        private boolean visible = true;
    }

    private static class AggregateSnapshot {
        private final String currentState;
        private final Instant lastSeenAt;
        private final Instant lastVisibilityAt;

        private AggregateSnapshot(String currentState, Instant lastSeenAt, Instant lastVisibilityAt) {
            this.currentState = currentState;
            this.lastSeenAt = lastSeenAt;
            this.lastVisibilityAt = lastVisibilityAt;
        }
    }

    private static class PresenceTransition {
        private final Long userId;
        private final String previousState;
        private final String currentState;
        private final String reason;

        private PresenceTransition(Long userId, String previousState, String currentState, String reason) {
            this.userId = userId;
            this.previousState = previousState;
            this.currentState = currentState;
            this.reason = reason;
        }
    }
}
