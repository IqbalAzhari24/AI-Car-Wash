package com.carwash.backend.controller;

import com.carwash.backend.dto.ChangePasswordRequest;
import com.carwash.backend.dto.UpdatePhoneRequest;
import com.carwash.backend.dto.UserProfileDto;
import com.carwash.backend.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Self-service account endpoints. Every authenticated role (customer, clerk,
 * worker, owner) manages their own profile and password here — no role check
 * beyond being logged in, since each user only ever touches their own record.
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileDto me(Authentication auth) {
        return userProfileService.getProfile(userId(auth));
    }

    @PatchMapping
    public UserProfileDto updatePhone(@RequestBody UpdatePhoneRequest req, Authentication auth) {
        return userProfileService.updatePhone(userId(auth), req.getPhoneNumber());
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication auth) {
        userProfileService.changePassword(userId(auth), req.getCurrentPassword(), req.getNewPassword());
        return Map.of("message", "Password updated.");
    }

    private UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
