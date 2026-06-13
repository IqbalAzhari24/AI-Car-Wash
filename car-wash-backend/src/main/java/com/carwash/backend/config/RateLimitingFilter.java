package com.carwash.backend.config;

import com.carwash.backend.service.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();

        // 1. Authentication Gate (Login)
        if (path.startsWith("/api/v1/auth/login")) {
            String ipAddress = request.getRemoteAddr();
            if (!rateLimiterService.isLoginAllowed(ipAddress)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many login attempts. Please try again later.");
                return;
            }
        }

        // 2. Chat Processing Endpoint
        if (path.startsWith("/api/v1/timah/chat")) {
            // Assuming the JWT filter sets the user ID as the Principal name
            String userId = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            if (!rateLimiterService.isChatAllowed(userId)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Chat message limit exceeded. Please wait before sending more.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
