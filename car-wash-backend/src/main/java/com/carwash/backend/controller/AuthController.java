package com.carwash.backend.controller;

import com.carwash.backend.config.JwtService;
import com.carwash.backend.dto.LoginRequest;
import com.carwash.backend.dto.LoginResponse;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest,
                                               HttpServletRequest request) {
        if (!StringUtils.hasText(loginRequest.getEmail()) || !StringUtils.hasText(loginRequest.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        }

        // Rate limiting is already enforced by RateLimitingFilter before this method is reached.
        // Do NOT add a second increment here — that would halve the effective limit.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password."));
        }

        // Authentication succeeded; the user must exist.
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + loginRequest.getEmail()));

        String role = user.getRole().name();
        String userId = user.getId().toString();
        String jwtToken = jwtService.generateToken(userId, role);

        return ResponseEntity.ok(new LoginResponse(jwtToken, role, userId));
    }
}
