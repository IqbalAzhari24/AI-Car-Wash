package com.carwash.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Extracts the JWT from the WebSocket upgrade request's "token" query parameter
 * and stores it in the session attributes map. The STOMP channel interceptor
 * (JwtChannelInterceptor) reads it back on the CONNECT frame and sets the Principal.
 *
 * Two-step split is necessary because HTTP request parameters are only visible
 * at handshake time, not in subsequent STOMP frames.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String token = servletRequest.getParameter("token");
            if (token != null && !token.isBlank()) {
                attributes.put("jwtToken", token);
                log.debug("WS handshake: JWT stored in session attributes");
            }
        }
        // Always allow the upgrade — JWT validation happens on the STOMP CONNECT frame
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
