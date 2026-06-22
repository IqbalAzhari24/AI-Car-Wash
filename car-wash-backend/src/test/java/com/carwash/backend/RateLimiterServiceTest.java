package com.carwash.backend;

import com.carwash.backend.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    RateLimiterService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new RateLimiterService(redisTemplate);
    }

    // --- isChatAllowed ---

    @Test
    void isChatAllowed_returns_true_below_limit() {
        when(valueOps.increment(anyString())).thenReturn(10L);
        when(redisTemplate.getExpire(anyString())).thenReturn(240L); // TTL set, not -1
        assertThat(service.isChatAllowed("user-1")).isTrue();
    }

    @Test
    void isChatAllowed_returns_true_at_exactly_20() {
        when(valueOps.increment(anyString())).thenReturn(20L);
        when(redisTemplate.getExpire(anyString())).thenReturn(100L);
        assertThat(service.isChatAllowed("user-1")).isTrue();
    }

    @Test
    void isChatAllowed_returns_false_at_21st_message() {
        when(valueOps.increment(anyString())).thenReturn(21L);
        when(redisTemplate.getExpire(anyString())).thenReturn(100L);
        assertThat(service.isChatAllowed("user-1")).isFalse();
    }

    @Test
    void isChatAllowed_sets_ttl_on_first_increment() {
        // currentCount == 1 short-circuits the || so getExpire() is never called
        when(valueOps.increment(anyString())).thenReturn(1L);

        service.isChatAllowed("user-new");

        verify(redisTemplate).expire(anyString(), any());
    }

    // --- isLoginAllowed ---

    @Test
    void isLoginAllowed_returns_true_below_limit() {
        when(valueOps.increment(anyString())).thenReturn(3L);
        when(redisTemplate.getExpire(anyString())).thenReturn(800L);
        assertThat(service.isLoginAllowed("192.168.1.1")).isTrue();
    }

    @Test
    void isLoginAllowed_returns_false_at_6th_attempt() {
        when(valueOps.increment(anyString())).thenReturn(6L);
        when(redisTemplate.getExpire(anyString())).thenReturn(800L);
        assertThat(service.isLoginAllowed("192.168.1.1")).isFalse();
    }
}
