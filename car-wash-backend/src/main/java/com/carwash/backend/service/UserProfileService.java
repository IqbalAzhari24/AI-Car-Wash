package com.carwash.backend.service;

import com.carwash.backend.dto.UserProfileDto;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.UserRepository;
import com.carwash.backend.util.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Self-service account management — every authenticated role (customer, clerk,
 * worker, owner) manages their own profile and password through this service.
 */
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        return UserProfileDto.from(findUser(userId));
    }

    @Transactional
    public UserProfileDto updatePhone(UUID userId, String phoneNumber) {
        User user = findUser(userId);
        String phone = ValidationUtil.normalizePhone(phoneNumber);
        if (StringUtils.hasText(phone) && !ValidationUtil.isValidMalaysianPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phone number must be a valid Malaysian number starting with +60 (e.g. +60123456789).");
        }
        user.setPhoneNumber(StringUtils.hasText(phone) ? phone : null);
        userRepository.save(user);
        return UserProfileDto.from(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
