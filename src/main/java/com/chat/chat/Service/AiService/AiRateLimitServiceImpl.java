package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiRateLimitServiceImpl implements AiRateLimitService {

    private static final String CODE_MINUTE = "AI_RATE_LIMIT_MINUTE";
    private static final String CODE_DAILY = "AI_RATE_LIMIT_DAILY";
    private static final String MSG_MINUTE = "Has usado la ayuda de IA varias veces seguidas. Espera un momento e intentalo de nuevo.";
    private static final String MSG_DAILY = "Has alcanzado el limite diario de ayuda con IA.";
    private static final long DAY_WINDOW_MS = Duration.ofDays(1).toMillis();
    private static final long MINUTE_WINDOW_MS = Duration.ofMinutes(1).toMillis();

    private final AiProperties aiProperties;
    private final Object globalLock = new Object();
    private final WindowCounter globalMinuteCounter = new WindowCounter();
    private final WindowCounter globalDayCounter = new WindowCounter();
    private final Map<Long, UserUsageCounter> userCounters = new ConcurrentHashMap<>();

    public AiRateLimitServiceImpl(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public boolean puedeUsar(Long userId) {
        return checkUsage(userId).isAllowed();
    }

    @Override
    public void registrarUso(Long userId) {
        long now = System.currentTimeMillis();
        synchronized (globalLock) {
            globalMinuteCounter.add(now, MINUTE_WINDOW_MS);
            globalDayCounter.add(now, DAY_WINDOW_MS);
        }
        UserUsageCounter counter = userCounters.computeIfAbsent(safeUserId(userId), ignored -> new UserUsageCounter());
        synchronized (counter) {
            counter.minuteCounter.add(now, MINUTE_WINDOW_MS);
            counter.dayCounter.add(now, DAY_WINDOW_MS);
        }
    }

    @Override
    public AiRateLimitCheck checkUsage(Long userId) {
        if (!aiProperties.isRateLimitEnabled()) {
            return new AiRateLimitCheck(true, "OK", "OK");
        }

        long now = System.currentTimeMillis();
        UserUsageCounter counter = userCounters.computeIfAbsent(safeUserId(userId), ignored -> new UserUsageCounter());
        synchronized (counter) {
            counter.minuteCounter.cleanup(now, MINUTE_WINDOW_MS);
            counter.dayCounter.cleanup(now, DAY_WINDOW_MS);
            if (counter.minuteCounter.size() >= aiProperties.getMaxUsesPerUserPerMinute()) {
                return new AiRateLimitCheck(false, CODE_MINUTE, MSG_MINUTE);
            }
            if (counter.dayCounter.size() >= aiProperties.getMaxUsesPerUserPerDay()) {
                return new AiRateLimitCheck(false, CODE_DAILY, MSG_DAILY);
            }
        }

        synchronized (globalLock) {
            globalMinuteCounter.cleanup(now, MINUTE_WINDOW_MS);
            globalDayCounter.cleanup(now, DAY_WINDOW_MS);
            if (globalMinuteCounter.size() >= aiProperties.getMaxGlobalUsesPerMinute()) {
                return new AiRateLimitCheck(false, CODE_MINUTE, MSG_MINUTE);
            }
            if (globalDayCounter.size() >= aiProperties.getMaxGlobalUsesPerDay()) {
                return new AiRateLimitCheck(false, CODE_DAILY, MSG_DAILY);
            }
        }

        return new AiRateLimitCheck(true, "OK", "OK");
    }

    private Long safeUserId(Long userId) {
        return userId == null ? -1L : userId;
    }

    private static final class UserUsageCounter {
        private final WindowCounter minuteCounter = new WindowCounter();
        private final WindowCounter dayCounter = new WindowCounter();
    }

    private static final class WindowCounter {
        private final Deque<Long> timestamps = new ArrayDeque<>();

        private void add(long now, long windowMs) {
            cleanup(now, windowMs);
            timestamps.addLast(now);
        }

        private void cleanup(long now, long windowMs) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) {
                timestamps.pollFirst();
            }
        }

        private int size() {
            return timestamps.size();
        }
    }
}
