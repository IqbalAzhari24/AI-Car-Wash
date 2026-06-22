package com.carwash.backend;

import com.carwash.backend.entity.PasswordResetToken;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.PasswordResetTokenRepository;
import com.carwash.backend.repository.UserRepository;
import com.carwash.backend.service.EmailService;
import com.carwash.backend.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock EmailService emailService;

    // Use real BCrypt so confirmReset can actually verify password encoding
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks PasswordResetService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expiryMinutes", 15);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
    }

    // --- initiateReset ---

    @Test
    void initiateReset_unknown_email_is_silent_no_op() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        service.initiateReset("ghost@example.com");
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), anyInt());
    }

    @Test
    void initiateReset_invalidates_existing_tokens_before_creating_new_one() {
        User user = testUser();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.invalidateAllForUser(eq(user.getId()), any())).thenReturn(2);
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.initiateReset("user@example.com");

        verify(tokenRepository).invalidateAllForUser(eq(user.getId()), any(LocalDateTime.class));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void initiateReset_stores_hash_not_raw_token() {
        User user = testUser();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<String> resetLinkCaptor = ArgumentCaptor.forClass(String.class);
        service.initiateReset("user@example.com");

        verify(emailService).sendPasswordResetEmail(any(), resetLinkCaptor.capture(), eq(15));
        String resetLink = resetLinkCaptor.getValue();
        String rawToken = resetLink.substring(resetLink.indexOf("?token=") + 7);

        // Capture saved token and verify its hash ≠ raw token
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).isNotEqualTo(rawToken);
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64); // SHA-256 hex = 64 chars
    }

    // --- confirmReset ---

    @Test
    void confirmReset_bcrypt_encodes_new_password() {
        User user = testUser();
        PasswordResetToken token = new PasswordResetToken(user, "anyhash",
                LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(token));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.confirmReset("some-raw-token", "NewPass123!");

        assertThat(passwordEncoder.matches("NewPass123!", user.getPasswordHash())).isTrue();
    }

    @Test
    void confirmReset_marks_token_used() {
        User user = testUser();
        PasswordResetToken token = new PasswordResetToken(user, "anyhash",
                LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(token));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.confirmReset("some-raw-token", "NewPass123!");

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void confirmReset_throws_400_when_token_not_found() {
        when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset("bad-token", "NewPass123!"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid or has expired");
    }

    // --- helpers ---

    private User testUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("user@example.com");
        u.setPasswordHash(passwordEncoder.encode("OldPass123!"));
        u.setRole(User.UserRole.CUSTOMER);
        return u;
    }
}
