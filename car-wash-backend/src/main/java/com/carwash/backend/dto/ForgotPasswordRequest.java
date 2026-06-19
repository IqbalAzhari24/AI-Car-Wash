package com.carwash.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/v1/auth/forgot-password.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email
) {}
