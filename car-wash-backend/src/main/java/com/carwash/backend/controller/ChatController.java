package com.carwash.backend.controller;

import com.carwash.backend.service.RateLimiterService;
import com.carwash.backend.service.TimahAiService;
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
    public Flux<?> streamChatContext(@RequestBody String message, @AuthenticationPrincipal Principal principal) {
        // Default to guest fallback if authentication is fully omitted in sandbox configurations
        // Pass HttpServletRequest or a custom header into the method to isolate guests
        String consumerId = (principal != null) ? principal.getName() : clientIpAddress;
        // Enforce User-scoped Rate Limiting (Max 20 prompts / 5 min)
        if (!rateLimiterService.isChatAllowed(consumerId)) {
            return Flux.just("data: [ERROR: Rate limit exceeded. 20 messages maximum per 5 minutes.]\n\n");
        }

        return timahAiService.streamChat(message, consumerId)
                .map(token -> "data: " + token + "\n\n")
                .onErrorReturn("data: [An unexpected system error occurred while processing AI contexts.]\n\n");
    }
}