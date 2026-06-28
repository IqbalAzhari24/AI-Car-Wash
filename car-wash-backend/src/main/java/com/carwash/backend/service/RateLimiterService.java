package com.carwash.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

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

    private boolean isAllowed(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && (count == 1 || redisTemplate.getExpire(key) == -1)) {
            redisTemplate.expire(key, window);
        }
        return count != null && count <= limit;
    }
}