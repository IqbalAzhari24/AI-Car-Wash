package com.carwash.backend.dto;

import com.carwash.backend.entity.User;

public class CreateStaffRequest {
    private String email;
    private String phoneNumber;
    private User.UserRole role; // CLERK, WORKER, or OWNER

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public User.UserRole getRole() { return role; }
    public void setRole(User.UserRole role) { this.role = role; }
}
