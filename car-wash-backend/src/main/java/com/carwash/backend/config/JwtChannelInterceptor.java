package com.carwash.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * STOMP-layer JWT interceptor.
 *
 * On CONNECT frames:
 *   1. Reads JWT from the STOMP "Authorization: Bearer ..." header (primary path).
 *   2. Falls back to a "jwtToken" session attribute stored by JwtHandshakeInterceptor
 *      (kept for backward compatibility with older clients that put the token in the URL).
 *   3. If no valid JWT is present, the CONNECT is REJECTED — anonymous WebSocket
 *      sessions are not allowed. This prevents unauthenticated callers from
 *      consuming rate-limit budget shared under a single "anonymous" bucket.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtChannelInterceptor.class);
    private final JwtService jwtService;

    public JwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            log.warn("WS CONNECT rejected — no JWT provided");
            throw new MessageDeliveryException("Authentication required. Connect with Authorization: Bearer <token> in the STOMP CONNECT header.");
        }

        try {
            if (!jwtService.isTokenValid(token)) {
                log.warn("WS CONNECT rejected — expired or invalid JWT");
                throw new MessageDeliveryException("Token is expired or invalid.");
            }
            String userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            accessor.setUser(auth);
            log.debug("WS CONNECT authenticated: userId={} role={}", userId, role);
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WS CONNECT rejected — JWT parse error: {}", e.getMessage());
            throw new MessageDeliveryException("Invalid token.");
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        // Primary: Authorization header in the STOMP CONNECT frame
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // Fallback: token stored during HTTP handshake (URL query param path)
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null) {
            String t = (String) attrs.get("jwtToken");
            if (StringUtils.hasText(t)) return t;
        }
        return null;
    }
}
