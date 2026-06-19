package com.carwash.backend;

import com.carwash.backend.config.JwtService;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ponytail: @SpringBootTest + MockMvc — heavier than @WebMvcTest but skips
// fighting the security filter chain config. Switch to @WebMvcTest if startup time matters.
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthenticationManager authenticationManager;
    @MockBean JwtService jwtService;
    @MockBean UserRepository userRepository;

    @Test
    void login_returns_400_when_email_is_blank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "", "password", "secret"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns_400_when_password_is_blank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "user@test.com", "password", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns_401_on_wrong_credentials() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "user@test.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns_200_and_token_on_valid_credentials() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(User.Role.CUSTOMER);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any())).thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "user@test.com", "password", "correct"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}
