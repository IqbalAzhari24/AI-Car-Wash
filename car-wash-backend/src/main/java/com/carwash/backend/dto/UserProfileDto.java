package com.carwash.backend.dto;

import com.carwash.backend.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/** The authenticated user's own profile. Returned by GET /api/v1/users/me. */
public class UserProfileDto {
    private final UUID id;
    private final String email;
    private final String phoneNumber;
    private final String role;
    private final LocalDateTime createdAt;

    public UserProfileDto(UUID id, String email, String phoneNumber, String role, LocalDateTime createdAt) {
        this.id           = id;
        this.email        = email;
        this.phoneNumber  = phoneNumber;
        this.role         = role;
        this.createdAt    = createdAt;
    }

    public static UserProfileDto from(User u) {
        return new UserProfileDto(u.getId(), u.getEmail(), u.getPhoneNumber(), u.getRole().name(), u.getCreatedAt());
    }

    public UUID getId()                 { return id; }
    public String getEmail()             { return email; }
    public String getPhoneNumber()       { return phoneNumber; }
    public String getRole()              { return role; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
