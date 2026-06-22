package com.carwash.backend;

import com.carwash.backend.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // base64("test-jwt-secret-key-for-unit-testing-2024!!!")  ≥ 256 bits
    private static final String SECRET = "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdW5pdC10ZXN0aW5nLTIwMjQhISE=";
    private static final long EXPIRY_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRY_MS);
    }

    @Test
    void generateToken_subject_is_userId() {
        String token = jwtService.generateToken("user-123", "CUSTOMER");
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void generateToken_contains_role_claim() {
        String token = jwtService.generateToken("user-123", "OWNER");
        assertThat(jwtService.extractRole(token)).isEqualTo("OWNER");
    }

    @Test
    void isTokenValid_returns_true_for_fresh_token() {
        String token = jwtService.generateToken("user-abc", "CLERK");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returns_false_for_expired_token() {
        JwtService shortLivedService = new JwtService(SECRET, -1L);
        String token = shortLivedService.generateToken("user-abc", "CUSTOMER");
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returns_false_for_garbage_input() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    void isTokenValid_returns_false_for_blank_string() {
        assertThat(jwtService.isTokenValid("")).isFalse();
    }
}
