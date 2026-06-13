package com.carwash.backend.controller;

import com.carwash.backend.service.TimahAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class TimahChatController {

    private static final Logger log = LoggerFactory.getLogger(TimahChatController.class);

    private final TimahAiService timahAiService;
    private final SimpMessagingTemplate messagingTemplate;

    public TimahChatController(TimahAiService timahAiService, SimpMessagingTemplate messagingTemplate) {
        this.timahAiService = timahAiService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Receives user messages from /app/timah/chat.
     * Streams Gemini token-by-token to /user/queue/timah-reply.
     */
    @MessageMapping("/timah/chat")
    public void handleChat(Map<String, String> payload, Principal principal) {
        String userMessage = payload.getOrDefault("message", "");
        String userId = principal != null ? principal.getName() : "anonymous";

        log.info("Timah received message from user [{}]: {}", userId, userMessage);

        // Stream tokens back to the individual user's private queue
        timahAiService.streamChat(userMessage, userId)
            .subscribe(
                token -> {
                    messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/timah-reply",
                        Map.of("type", "TOKEN", "content", token)
                    );
                },
                error -> {
                    log.error("Error streaming from Gemini: ", error);
                    messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/timah-reply",
                        Map.of("type", "ERROR", "content", "Timah is temporarily unavailable. Please try again.")
                    );
                },
                () -> {
                    // Signal stream completion to frontend
                    messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/timah-reply",
                        Map.of("type", "DONE", "content", "")
                    );
                    log.info("Stream complete for user [{}]", userId);
                }
            );
    }
}
