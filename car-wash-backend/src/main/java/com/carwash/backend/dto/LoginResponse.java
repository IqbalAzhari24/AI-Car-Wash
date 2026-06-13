package com.carwash.backend.dto;

public class LoginResponse {
    private final String token;
    private final String role;
    private final String userId;

    public LoginResponse(String token, String role, String userId) {
        this.token = token;
        this.role = role;
        this.userId = userId;
    }

    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getUserId() { return userId; }
}
