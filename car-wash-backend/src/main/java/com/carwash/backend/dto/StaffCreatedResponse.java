package com.carwash.backend.dto;

import java.util.UUID;

/**
 * Returned once after staff creation. The tempPassword is shown only here
 * and never stored — the owner must share it with the new staff member immediately.
 */
public class StaffCreatedResponse {
    private final UUID userId;
    private final String email;
    private final String role;
    private final String tempPassword;

    public StaffCreatedResponse(UUID userId, String email, String role, String tempPassword) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.tempPassword = tempPassword;
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getTempPassword() { return tempPassword; }
}
