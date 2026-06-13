package com.carwash.backend.config;

import com.carwash.backend.service.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketRateLimitInterceptor.class);
    private final RateLimiterService rateLimiterService;

    public WebSocketRateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Intercept standard SEND frames targeted at our MessageMapping destinations
        if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            // Validate specifically for the chat channel routing path
            if (destination != null && destination.contains("/timah/chat")) {
                Principal principal = accessor.getUser();
                String userId = (principal != null) ? principal.getName() : "anonymous";

                // Query your existing Redis engine
                if (!rateLimiterService.isChatAllowed(userId)) {
                    log.warn("Rate limit triggered on WebSocket channel for user ID: {}", userId);
                    
                    // Rejecting the frame prevents downstream processing by TimahChatController
                    throw new IllegalArgumentException("Rate limit exceeded. 20 messages maximum per 5 minutes.");
                }
            }
        }
        return message;
    }
}
