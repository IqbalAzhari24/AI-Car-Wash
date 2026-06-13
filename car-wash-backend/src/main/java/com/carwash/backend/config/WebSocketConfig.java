package com.carwash.backend.config;

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

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                           JwtChannelInterceptor jwtChannelInterceptor,
                           WebSocketRateLimitInterceptor rateLimitInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
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
                .setAllowedOrigins("http://localhost:5173")
                .addInterceptors(jwtHandshakeInterceptor) // stash JWT in session attrs at HTTP upgrade
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT auth first so principal is populated before the rate limiter reads accessor.getUser()
        registration.interceptors(jwtChannelInterceptor, rateLimitInterceptor);
    }
}