package com.carwash.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RateLimiterService {

    /**
     * Atomic Lua script — increment and set TTL in a single Redis operation.
     *
     * KEYS[1] = the rate-limit key
     * ARGV[1] = window size in seconds
     *
     * Returns the new count after incrementing.
     * The TTL is only set on the first call (count == 1) so the window is
     * anchored to the first request and cannot be reset by repeated calls.
     */
    private static final DefaultRedisScript<Long> INCR_AND_EXPIRE = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return count",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Limit strictly to 20 messages per 5 minutes per authenticated JWT User ID. */
    public boolean isChatAllowed(String userId) {
        return isAllowed("rate_limit:chat:" + userId, 20, Duration.ofMinutes(5));
    }

    /** Limit strictly to 5 submission attempts per 15 minutes per inbound IP Address. */
    public boolean isLoginAllowed(String ipAddress) {
        return isAllowed("rate_limit:login:" + ipAddress, 5, Duration.ofMinutes(15));
    }

    /**
     * Atomically increments the request counter and enforces the window TTL.
     * Uses a Lua script so the increment and expire execute as a single Redis command —
     * no race window between the two operations.
     */
    private boolean isAllowed(String key, int limit, Duration window) {
        Long count = redisTemplate.execute(
                INCR_AND_EXPIRE,
                List.of(key),
                String.valueOf(window.getSeconds())
        );
        return count != null && count <= limit;
    }
}