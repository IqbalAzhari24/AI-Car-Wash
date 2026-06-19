package com.carwash.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final WebSocketRateLimitInterceptor rateLimitInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                           JwtChannelInterceptor jwtChannelInterceptor,
                           WebSocketRateLimitInterceptor rateLimitInterceptor,
                           @Value("${cors.allowed-origins:http://localhost:5173}") String allowedOriginsStr) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.allowedOrigins = allowedOriginsStr.split("\\s*,\\s*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/timah")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(jwtHandshakeInterceptor) // stash JWT in session attrs at HTTP upgrade
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT auth first so principal is populated before the rate limiter reads accessor.getUser()
        registration.interceptors(jwtChannelInterceptor, rateLimitInterceptor);
    }
}