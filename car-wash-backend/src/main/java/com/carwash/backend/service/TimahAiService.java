package com.carwash.backend.service;

import com.carwash.backend.dto.BookingDto;
import com.carwash.backend.dto.CreateBookingRequest;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.ShopClosure;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.ShopClosureRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Timah AI service — Gemini 1.5 Flash with function/tool-calling.
 *
 * Flow:
 *   Turn 1 (non-streaming): send user message + tool declarations to generateContent.
 *   → If Gemini returns a functionCall: execute the tool locally, then
 *   Turn 2 (streaming): send conversation history + functionResponse to streamGenerateContent.
 *   → After streaming completes: if create_booking succeeded, append a REDIRECT_CHECKOUT
 *     JSON token that the frontend hook intercepts to navigate to /checkout/:bookingId.
 *   → If Turn 1 returned plain text: emit it directly (no second call needed).
 */
@Service
public class TimahAiService {

    private static final Logger log = LoggerFactory.getLogger(TimahAiService.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash";
    private static final String GENERATE_URL = BASE_URL + ":generateContent";
    private static final String STREAM_URL    = BASE_URL + ":streamGenerateContent";

    private final WebClient webClient;
    private final SlotCapacityRepository slotCapacityRepository;
    private final ShopClosureRepository shopClosureRepository;
    private final BookingService bookingService;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${gemini.api.key:GEMINI_API_KEY_NOT_SET}")
    private String geminiApiKey;

    public TimahAiService(WebClient.Builder webClientBuilder,
                          SlotCapacityRepository slotCapacityRepository,
                          ShopClosureRepository shopClosureRepository,
                          BookingService bookingService) {
        this.webClient = webClientBuilder.build();
        this.slotCapacityRepository = slotCapacityRepository;
        this.shopClosureRepository = shopClosureRepository;
        this.bookingService = bookingService;
    }

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /**
     * @param userMessage The raw message from the customer.
     * @param userId      The authenticated user's UUID string, or "anonymous".
     */
    public Flux<String> streamChat(String userMessage, String userId) {
        String systemPrompt = buildSystemPrompt();
        ObjectNode body = buildTurn1Body(systemPrompt, userMessage);

        return webClient.post()
                .uri(GENERATE_URL)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", geminiApiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(resp -> routeResponse(resp, systemPrompt, userMessage, userId))
                .onErrorResume(e -> {
                    log.error("Gemini error: {}", e.getMessage());
                    return Flux.just("I'm having trouble connecting right now. Please try again shortly.");
                });
    }

    // -------------------------------------------------------------------------
    // Response routing
    // -------------------------------------------------------------------------

    private Flux<String> routeResponse(JsonNode resp, String systemPrompt,
                                        String userMessage, String userId) {
        JsonNode candidates = resp.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return Flux.just("Sorry, I didn't get a response. Please try again.");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return Flux.just("Sorry, I didn't get a response. Please try again.");
        }

        JsonNode firstPart = parts.get(0);
        if (firstPart.has("functionCall")) {
            return handleFunctionCall(firstPart.path("functionCall"), resp,
                                      systemPrompt, userMessage, userId);
        }

        // Plain text — collect all parts and emit
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : parts) {
            sb.append(p.path("text").asText(""));
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? Flux.empty() : Flux.just(text);
    }

    // -------------------------------------------------------------------------
    // Function-call handling
    // -------------------------------------------------------------------------

    private Flux<String> handleFunctionCall(JsonNode functionCall, JsonNode turn1Resp,
                                             String systemPrompt, String userMessage, String userId) {
        String name = functionCall.path("name").asText();
        JsonNode args = functionCall.path("args");
        log.info("Timah tool call: {} args={}", name, args);

        ToolResult result = executeTool(name, args, userId);

        ObjectNode turn2Body = buildTurn2Body(systemPrompt, userMessage,
                                              turn1Resp, name, result.responseNode);

        return webClient.post()
                .uri(STREAM_URL + "?key=" + geminiApiKey + "&alt=sse")
                .header("Content-Type", "application/json")
                .bodyValue(turn2Body)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractText)
                .filter(t -> !t.isEmpty())
                .concatWith(
                    // Append redirect signal after the stream ends if booking was created
                    result.bookingId != null
                        ? Flux.just(redirectJson(result.bookingId))
                        : Flux.empty()
                );
    }

    // -------------------------------------------------------------------------
    // Tool implementations
    // -------------------------------------------------------------------------

    private ToolResult executeTool(String name, JsonNode args, String userId) {
        try {
            switch (name) {
                case "get_available_slots": return toolGetAvailableSlots(args);
                case "create_booking":      return toolCreateBooking(args, userId);
                default:
                    return ToolResult.error("Unknown tool: " + name);
            }
        } catch (Exception e) {
            log.error("Tool '{}' threw: {}", name, e.getMessage());
            return ToolResult.error(e.getMessage());
        }
    }

    private ToolResult toolGetAvailableSlots(JsonNode args) {
        LocalDate startDate = LocalDate.now();
        if (args.has("date")) {
            try { startDate = LocalDate.parse(args.path("date").asText()); }
            catch (Exception ignored) { }
        }
        int days = args.has("days") ? Math.min(args.path("days").asInt(3), 7) : 3;

        ArrayNode slots = om.createArrayNode();
        for (int i = 0; i < days; i++) {
            LocalDate day = startDate.plusDays(i);
            List<SlotCapacity> daySlots = slotCapacityRepository
                    .findBySlotTimeBetween(day.atStartOfDay(), day.atTime(LocalTime.MAX));
            for (SlotCapacity s : daySlots) {
                int avail = s.getMaxLimit() - s.getBookedCount();
                if (avail > 0) {
                    slots.addObject()
                            .put("slotTime", s.getSlotTime().toString())
                            .put("available", avail)
                            .put("capacity", s.getMaxLimit());
                }
            }
        }

        ObjectNode resp = om.createObjectNode();
        resp.set("availableSlots", slots);
        resp.put("totalAvailable", slots.size());
        return ToolResult.ok(resp);
    }

    private ToolResult toolCreateBooking(JsonNode args, String userId) {
        if ("anonymous".equals(userId)) {
            return ToolResult.ok(om.createObjectNode()
                    .put("success", false)
                    .put("error", "Customer must be logged in to create a booking."));
        }

        // --- Validate and parse slotTime ---
        String slotTimeStr = args.path("slotTime").asText("");
        LocalDateTime slotTime;
        try {
            slotTime = LocalDateTime.parse(slotTimeStr);
        } catch (Exception e) {
            return ToolResult.ok(om.createObjectNode()
                    .put("success", false)
                    .put("error", "Invalid slotTime '" + slotTimeStr + "'. Use ISO-8601, e.g. 2026-06-14T10:00:00"));
        }

        // --- Validate vehicleClass ---
        String vcStr = args.path("vehicleClass").asText("SEDAN").toUpperCase();
        Booking.VehicleClass vehicleClass;
        try {
            vehicleClass = Booking.VehicleClass.valueOf(vcStr);
        } catch (IllegalArgumentException e) {
            return ToolResult.ok(om.createObjectNode()
                    .put("success", false)
                    .put("error", "Invalid vehicleClass '" + vcStr
                            + "'. Must be one of: MOTORCYCLE, COMPACT, SEDAN, SUV_LUXURY, MPV_LARGE"));
        }

        String vehicleModel = args.path("vehicleModel").asText("Unknown").trim();

        // --- Call BookingService ---
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSlotTime(slotTime);
        req.setVehicleClass(vehicleClass);
        req.setVehicleModel(vehicleModel);

        try {
            BookingDto dto = bookingService.createBooking(UUID.fromString(userId), false, req);
            ObjectNode resp = om.createObjectNode()
                    .put("success", true)
                    .put("bookingId", dto.getId().toString())
                    .put("totalPrice", dto.getTotalPrice().toPlainString())
                    .put("slotTime", dto.getSlotTime().toString())
                    .put("status", dto.getStatus());
            return new ToolResult(dto.getId(), resp);
        } catch (ResponseStatusException e) {
            return ToolResult.ok(om.createObjectNode()
                    .put("success", false)
                    .put("error", e.getReason() != null ? e.getReason() : "Booking failed."));
        }
    }

    // -------------------------------------------------------------------------
    // Request builders
    // -------------------------------------------------------------------------

    private String buildSystemPrompt() {
        LocalDate today = LocalDate.now();
        LocalDateTime sod = today.atStartOfDay();

        List<SlotCapacity> todaySlots = slotCapacityRepository
                .findBySlotTimeBetween(sod, today.atTime(LocalTime.MAX));
        List<ShopClosure> closures = shopClosureRepository.findByEndTimeAfter(sod);

        String capacityCtx = todaySlots.stream()
                .map(s -> "  " + s.getSlotTime() + " — "
                        + (s.getMaxLimit() - s.getBookedCount()) + " of " + s.getMaxLimit() + " free")
                .collect(Collectors.joining("\n"));

        String closureCtx = closures.stream()
                .map(c -> "  " + c.getStartTime() + " to " + c.getEndTime() + ": " + c.getReason())
                .collect(Collectors.joining("\n"));

        return "You are Timah, the friendly AI booking assistant for Timah Car Wash.\n"
                + "Today is " + today + ". Operating hours: 09:00–18:00, closed on Fridays.\n\n"
                + "Today's availability:\n" + (capacityCtx.isEmpty() ? "  No slots today." : capacityCtx) + "\n\n"
                + "Upcoming closures:\n" + (closureCtx.isEmpty() ? "  None — fully operational." : closureCtx) + "\n\n"
                + "Vehicle classes and pricing (base RM 25 for SEDAN):\n"
                + "  MOTORCYCLE — RM 15, 1 slot (30 min)\n"
                + "  COMPACT    — RM 20, 1 slot (30 min)\n"
                + "  SEDAN      — RM 25, 1 slot (30 min)\n"
                + "  SUV_LUXURY — RM 40, 2 consecutive slots (60 min)\n"
                + "  MPV_LARGE  — RM 50, 3 consecutive slots (90 min)\n\n"
                + "When a customer asks to book:\n"
                + "  1. Ask for preferred date/time if not given.\n"
                + "  2. Call get_available_slots to confirm availability.\n"
                + "  3. Confirm the exact slot, vehicle class, and vehicle model with the customer.\n"
                + "  4. Only then call create_booking.\n"
                + "Be warm and concise. Respond in the same language the customer uses.";
    }

    /** Turn 1: user message + tool declarations, non-streaming. */
    private ObjectNode buildTurn1Body(String systemPrompt, String userMessage) {
        ObjectNode body = om.createObjectNode();

        ObjectNode sys = body.putObject("systemInstruction");
        sys.putArray("parts").addObject().put("text", systemPrompt);

        ArrayNode contents = body.putArray("contents");
        ObjectNode userTurn = contents.addObject();
        userTurn.put("role", "user");
        userTurn.putArray("parts").addObject().put("text", userMessage);

        body.set("tools", toolDeclarations());

        ObjectNode cfg = body.putObject("generationConfig");
        cfg.put("temperature", 0.4);
        cfg.put("maxOutputTokens", 1024);

        return body;
    }

    /**
     * Turn 2: full conversation history (user → model functionCall → functionResponse)
     * sent for streaming final answer.
     */
    private ObjectNode buildTurn2Body(String systemPrompt, String userMessage,
                                       JsonNode turn1Resp, String toolName, JsonNode toolResult) {
        ObjectNode body = om.createObjectNode();

        ObjectNode sys = body.putObject("systemInstruction");
        sys.putArray("parts").addObject().put("text", systemPrompt);

        ArrayNode contents = body.putArray("contents");

        // [1] User's original message
        ObjectNode userTurn = contents.addObject();
        userTurn.put("role", "user");
        userTurn.putArray("parts").addObject().put("text", userMessage);

        // [2] Model's function-call turn (verbatim from turn 1 response)
        contents.add(turn1Resp.path("candidates").get(0).path("content"));

        // [3] Tool result — sent as a user turn with functionResponse part
        ObjectNode funcTurn = contents.addObject();
        funcTurn.put("role", "user");
        ObjectNode funcResp = funcTurn.putArray("parts").addObject().putObject("functionResponse");
        funcResp.put("name", toolName);
        funcResp.set("response", toolResult);

        body.set("tools", toolDeclarations());

        ObjectNode cfg = body.putObject("generationConfig");
        cfg.put("temperature", 0.4);
        cfg.put("maxOutputTokens", 1024);

        return body;
    }

    private ArrayNode toolDeclarations() {
        ArrayNode tools = om.createArrayNode();
        ArrayNode decls = tools.addObject().putArray("functionDeclarations");

        // --- get_available_slots ---
        ObjectNode getSlots = decls.addObject();
        getSlots.put("name", "get_available_slots");
        getSlots.put("description",
                "Returns available car wash time slots for a date range. "
                + "Call this before confirming a booking to show the customer real-time availability.");
        ObjectNode getSlotsParams = getSlots.putObject("parameters");
        getSlotsParams.put("type", "OBJECT");
        ObjectNode getSlotsProps = getSlotsParams.putObject("properties");
        getSlotsProps.putObject("date")
                .put("type", "STRING")
                .put("description", "Start date in YYYY-MM-DD format. Defaults to today if omitted.");
        getSlotsProps.putObject("days")
                .put("type", "INTEGER")
                .put("description", "Number of days to check starting from 'date'. Defaults to 3, max 7.");

        // --- create_booking ---
        ObjectNode createBook = decls.addObject();
        createBook.put("name", "create_booking");
        createBook.put("description",
                "Creates a confirmed PENDING booking for the logged-in customer. "
                + "Only call this after the customer has explicitly confirmed the slot, vehicle class, and vehicle model.");
        ObjectNode cbParams = createBook.putObject("parameters");
        cbParams.put("type", "OBJECT");
        ObjectNode cbProps = cbParams.putObject("properties");
        cbProps.putObject("slotTime")
                .put("type", "STRING")
                .put("description", "Booking date/time in ISO-8601 format e.g. 2026-06-14T10:00:00");
        cbProps.putObject("vehicleClass")
                .put("type", "STRING")
                .put("description",
                        "Vehicle class — must be exactly one of: MOTORCYCLE, COMPACT, SEDAN, SUV_LUXURY, MPV_LARGE");
        cbProps.putObject("vehicleModel")
                .put("type", "STRING")
                .put("description", "Vehicle make and model, e.g. Honda City 2023, Toyota Hilux");
        cbParams.putArray("required")
                .add("slotTime").add("vehicleClass").add("vehicleModel");

        return tools;
    }

    // -------------------------------------------------------------------------
    // SSE chunk parser
    // -------------------------------------------------------------------------

    private String extractText(String rawChunk) {
        try {
            String json = rawChunk.startsWith("data: ") ? rawChunk.substring(6) : rawChunk;
            JsonNode root = om.readTree(json);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("");
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse chunk: {}", rawChunk);
        }
        return "";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String redirectJson(UUID bookingId) {
        return "{\"action\":\"REDIRECT_CHECKOUT\",\"bookingId\":\"" + bookingId + "\"}";
    }

    private static class ToolResult {
        final UUID bookingId;      // non-null only when create_booking succeeded
        final JsonNode responseNode;

        ToolResult(UUID bookingId, JsonNode responseNode) {
            this.bookingId = bookingId;
            this.responseNode = responseNode;
        }

        static ToolResult ok(JsonNode node) { return new ToolResult(null, node); }
        static ToolResult error(String msg) {
            ObjectMapper m = new ObjectMapper();
            return new ToolResult(null, m.createObjectNode().put("error", msg));
        }
    }
}
