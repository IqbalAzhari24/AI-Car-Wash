package com.carwash.backend.dto;

import com.carwash.backend.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDto {
    private final UUID id;
    private final String email;
    private final String phoneNumber;
    private final String role;
    private final LocalDateTime createdAt;

    public UserDto(UUID id, String email, String phoneNumber, String role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getCreatedAt());
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
