package com.carwash.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // Replaces the deprecated @EnableGlobalMethodSecurity
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter; // Inject your rate limiter properly

    public WebSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, 
                             RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> response.setStatus(HttpStatus.FORBIDDEN.value());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Spring Boot's error page is reached via an internal Tomcat FORWARD
                // whenever a handler calls response.sendError(...) (e.g. any
                // ResponseStatusException). That forwarded request re-enters this same
                // filter chain without the original Authorization header, so without
                // this rule every non-2xx sendError response (409, 404, etc.) got its
                // status clobbered by the 401 entry point on the forwarded dispatch.
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/payments/toyyibpay/callback").permitAll() // gateway server-to-server
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/ws/**").permitAll() // Open for initial handshake only
                .requestMatchers("/api/v1/owner/**").hasRole("OWNER")
                .anyRequest().authenticated()
            )
            // Unauthenticated requests -> 401. Authenticated-but-wrong-role requests -> 403,
            // forced explicitly: without this, ExceptionTranslationFilter was routing
            // AccessDeniedException to the authenticationEntryPoint (401) instead of a 403
            // handler for every role mismatch, method-security or URL-matcher alike.
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(accessDeniedHandler()))
            // 1. Enforce Rate Limiting FIRST before anything else processes
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            // 2. Extract JWT parameters next
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}