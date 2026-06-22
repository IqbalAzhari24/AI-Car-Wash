package com.carwash.backend.controller;

import com.carwash.backend.service.RateLimiterService;
import com.carwash.backend.service.TimahAiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final TimahAiService timahAiService;
    private final RateLimiterService rateLimiterService;

    public ChatController(TimahAiService timahAiService, RateLimiterService rateLimiterService) {
        this.timahAiService = timahAiService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Streams multi-part operational content back from Gemini via SSE.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<?> streamChatContext(@RequestBody String message,
                                     @AuthenticationPrincipal Principal principal,
                                     HttpServletRequest request) {
        // Default to guest fallback if authentication is fully omitted in sandbox configurations
        // Use the request IP to isolate unauthenticated guests for rate limiting.
        String userId = (principal != null) ? principal.getName() : "anonymous";
        String consumerId = (principal != null) ? principal.getName() : request.getRemoteAddr();
        // Enforce User-scoped Rate Limiting (Max 20 prompts / 5 min)
        if (!rateLimiterService.isChatAllowed(consumerId)) {
            return Flux.just("data: [ERROR: Rate limit exceeded. 20 messages maximum per 5 minutes.]\n\n");
        }

        return timahAiService.streamChat(message, userId)
                .map(token -> "data: " + token + "\n\n")
                .onErrorReturn("data: [An unexpected system error occurred while processing AI contexts.]\n\n");
    }
}
