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

    /**
     * Limit strictly to 20 messages per 5 minutes per authenticated JWT User ID.
     */
    public boolean isChatAllowed(String userId) {
        String key = "rate_limit:chat:" + userId;
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        // Self-Healing Fallback: Checks if key exists but lacks an active TTL (-1)
        if (currentCount != null && (currentCount == 1 || redisTemplate.getExpire(key) == -1)) {
            redisTemplate.expire(key, Duration.ofMinutes(5));
        }
        
        return currentCount != null && currentCount <= 20;
    }

    /**
     * Limit strictly to 5 submission attempts per 15 minutes mapped per inbound IP Address.
     */
    public boolean isLoginAllowed(String ipAddress) {
        String key = "rate_limit:login:" + ipAddress;
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        // Self-Healing Fallback: Handles edge-case connection interruptions seamlessly
        if (currentCount != null && (currentCount == 1 || redisTemplate.getExpire(key) == -1)) {
            redisTemplate.expire(key, Duration.ofMinutes(15));
        }
        
        return currentCount != null && currentCount <= 5;
    }
}