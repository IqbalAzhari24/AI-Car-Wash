package com.carwash.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring's JavaMailSender.
 * Configure SMTP credentials via environment variables — never hardcode them.
 *
 * Required application.yml entries:
 * <pre>
 * spring:
 *   mail:
 *     host: smtp.mailtrap.io       # or smtp.gmail.com for production
 *     port: 587
 *     username: ${MAIL_USER}
 *     password: ${MAIL_PASS}
 *     properties:
 *       mail.smtp.auth: true
 *       mail.smtp.starttls.enable: true
 * azurewash:
 *   mail:
 *     from: noreply@azurewash.my
 * </pre>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${azurewash.mail.from:noreply@azurewash.my}")
    private String fromAddress;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a password reset link to the given email address.
     *
     * @param toEmail      recipient's email
     * @param resetLink    the full reset URL containing the raw token
     * @param expiryMinutes how many minutes the link remains valid
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink, int expiryMinutes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("AzureWash — Reset Your Password");
            message.setText(
                    "Hello,\n\n" +
                    "We received a request to reset your AzureWash password.\n\n" +
                    "Click the link below to set a new password (valid for " + expiryMinutes + " minutes):\n\n" +
                    resetLink + "\n\n" +
                    "If you did not request this, you can safely ignore this email.\n\n" +
                    "— The AzureWash Team"
            );
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            // Log but do not propagate — a mail failure must not expose whether the email exists
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
