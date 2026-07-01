package com.carwash.backend.service;

import com.carwash.backend.entity.PasswordResetToken;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.PasswordResetTokenRepository;
import com.carwash.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Handles the two-step forgot-password flow.
 *
 * <p>Step 1 — {@link #initiateReset(String)}: validates the email, invalidates any existing
 * open tokens, generates a short-lived cryptographically secure token, stores its SHA-256 hash,
 * and fires a reset email. Returns silently regardless of whether the email is registered
 * (prevents email enumeration).
 *
 * <p>Step 2 — {@link #confirmReset(String, String)}: hashes the incoming raw token, looks up
 * the matching DB record, enforces expiry and single-use, then BCrypt-encodes and saves the
 * new password.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    @Value("${azurewash.password-reset.expiry-minutes:15}")
    private int expiryMinutes;

    @Value("${azurewash.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // -------------------------------------------------------------------------
    // Step 1 — initiate
    // -------------------------------------------------------------------------

    /**
     * Initiates password reset for the given email.
     * Always returns without error, even if the email is not registered.
     *
     * @param email the address submitted by the user
     */
    @Transactional
    public void initiateReset(String email) {
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {
            int invalidated = tokenRepository.invalidateAllForUser(user.getId(), LocalDateTime.now());
            if (invalidated > 0) {
                log.info("Invalidated {} existing reset token(s) for user {}", invalidated, user.getId());
            }

            String rawToken  = generateSecureToken();
            String tokenHash = sha256Hex(rawToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

            tokenRepository.save(new PasswordResetToken(user, tokenHash, expiresAt));

            String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink, expiryMinutes);

            log.info("Password reset email dispatched for user {}", user.getId());
        });
        // Intentional no-op when email is not found — prevents enumeration
    }

    // -------------------------------------------------------------------------
    // Step 2 — confirm
    // -------------------------------------------------------------------------

    /**
     * Confirms the password reset using the raw token from the email link.
     *
     * @param rawToken    the plain token value
     * @param newPassword the new plain-text password chosen by the user
     * @throws ResponseStatusException 400 if the token is invalid, expired, or already used
     */
    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        String tokenHash = sha256Hex(rawToken);

        PasswordResetToken resetToken = tokenRepository
                .findValidToken(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("Password reset attempted with invalid or expired token");
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is invalid or has expired");
                });

        User user = resetToken.getUser();
        // Note: the User entity stores the hashed password in the 'passwordHash' column
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.markUsed();
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user {}", user.getId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Generates a 32-byte cryptographically secure URL-safe Base64 token. */
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Returns the SHA-256 hex digest of a UTF-8 encoded string. */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
