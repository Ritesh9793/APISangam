package com.apimarketplace.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final long windowSeconds;
    private final DefaultRedisScript<String> slidingWindowScript;
    private final ConcurrentHashMap<String, Window> fallbackWindows = new ConcurrentHashMap<>();

    public RateLimitService(
        StringRedisTemplate redisTemplate,
        @Value("${app.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.windowSeconds = windowSeconds;
        this.slidingWindowScript = buildScript();
    }

    public RateLimitDecision allow(String key, int limitPerWindow) {
        return allow(key, limitPerWindow, windowSeconds);
    }

    public RateLimitDecision allow(String key, int limitPerWindow, long windowSeconds) {
        try {
            return allowWithRedis(key, limitPerWindow, windowSeconds);
        } catch (DataAccessException ex) {
            return allowWithFallback(key, limitPerWindow, windowSeconds);
        }
    }

    private RateLimitDecision allowWithRedis(String key, int limitPerWindow, long windowSeconds) {
        String redisKey = "rate_limit:" + key;
        long nowMillis = Instant.now().toEpochMilli();
        String member = nowMillis + ":" + Thread.currentThread().getId();
        String result = redisTemplate.execute(
            slidingWindowScript,
            List.of(redisKey),
            String.valueOf(nowMillis),
            String.valueOf(windowSeconds),
            String.valueOf(limitPerWindow),
            member
        );

        if (result == null || result.isBlank()) {
            return allowWithFallback(key, limitPerWindow, windowSeconds);
        }

        String[] parts = result.split(":");
        boolean allowed = "1".equals(parts[0]);
        int remaining = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        long retryAfter = parts.length > 2 ? Long.parseLong(parts[2]) : 0L;
        return new RateLimitDecision(allowed, remaining, retryAfter);
    }

    private RateLimitDecision allowWithFallback(String key, int limitPerWindow, long windowSeconds) {
        long currentWindow = Instant.now().getEpochSecond() / Math.max(1L, windowSeconds);
        Window window = fallbackWindows.computeIfAbsent(key, ignored -> new Window(currentWindow));

        synchronized (window) {
            if (window.windowEpoch != currentWindow) {
                window.windowEpoch = currentWindow;
                window.count.set(0);
            }

            int current = window.count.incrementAndGet();
            int remaining = Math.max(0, limitPerWindow - current);
            long retryAfterSeconds = remaining > 0 ? 0 : Math.max(1L, windowSeconds - (Instant.now().getEpochSecond() % windowSeconds));
            return new RateLimitDecision(current <= limitPerWindow, remaining, retryAfterSeconds);
        }
    }

    private DefaultRedisScript<String> buildScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, now - (window * 1000))
            local count = redis.call('ZCARD', key)
            if count < limit then
              redis.call('ZADD', key, now, member)
              redis.call('EXPIRE', key, window + 1)
              return "1:" .. tostring(limit - count - 1) .. ":0"
            end
            local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
            local retry = window
            if oldest[2] then
              retry = math.ceil((tonumber(oldest[2]) + (window * 1000) - now) / 1000)
              if retry < 0 then
                retry = 0
              end
            end
            return "0:0:" .. tostring(retry)
            """);
        return script;
    }

    public record RateLimitDecision(boolean allowed, int remainingRequests, long retryAfterSeconds) {}

    private static final class Window {
        private volatile long windowEpoch;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long windowEpoch) {
            this.windowEpoch = windowEpoch;
        }
    }
}
