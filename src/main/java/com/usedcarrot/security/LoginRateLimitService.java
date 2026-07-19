package com.usedcarrot.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimitService {
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    @Autowired
    public LoginRateLimitService(@Value("${usedcarrot.login-rate-limit.max-failures:10}") int maxFailures,
                                 @Value("${usedcarrot.login-rate-limit.window-seconds:60}") long windowSeconds) {
        this(maxFailures, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    LoginRateLimitService(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public boolean isBlocked(String clientIp) {
        Deque<Instant> attempts = failures.get(clientIp);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            removeExpired(attempts);
            return attempts.size() >= maxFailures;
        }
    }

    public void recordFailure(String clientIp) {
        Deque<Instant> attempts = failures.computeIfAbsent(clientIp, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            removeExpired(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void clear(String clientIp) {
        failures.remove(clientIp);
    }

    private void removeExpired(Deque<Instant> attempts) {
        Instant cutoff = clock.instant().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.removeFirst();
        }
    }
}
