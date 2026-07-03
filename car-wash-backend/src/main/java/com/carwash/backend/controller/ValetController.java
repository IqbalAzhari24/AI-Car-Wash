package com.carwash.backend.controller;

import com.carwash.backend.dto.CreateValetRequestDto;
import com.carwash.backend.dto.ValetRequestDto;
import com.carwash.backend.entity.ValetRequest;
import com.carwash.backend.service.ValetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for valet pick-up requests.
 *
 * <p>Base path: {@code /api/v1/valet}
 *
 * <pre>
 * POST   /api/v1/valet/requests               — CUSTOMER: submit a new request
 * GET    /api/v1/valet/requests/mine          — CUSTOMER: view own requests
 * GET    /api/v1/valet/requests               — CLERK|OWNER: view all requests for a branch
 * PATCH  /api/v1/valet/requests/{id}/status   — CLERK|OWNER: advance / reject / cancel
 * PATCH  /api/v1/valet/requests/{id}/cancel   — CUSTOMER: cancel own request
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/valet")
public class ValetController {

    private final ValetService valetService;

    public ValetController(ValetService valetService) {
        this.valetService = valetService;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/valet/requests — CUSTOMER submits a pick-up request
    // -------------------------------------------------------------------------

    /**
     * Submit a new valet pick-up request.
     *
     * <p>The customer's GPS coordinates (from {@code navigator.geolocation})
     * are validated server-side using the Haversine formula.
     * Response includes {@code status} (ACCEPTED or REJECTED) and
     * {@code distanceKm} / {@code radiusKm} for the frontend to display.
     */
    @PostMapping("/requests")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> submitRequest(
            Authentication authentication,
            @RequestBody CreateValetRequestDto dto) {

        ValetRequestDto result = valetService.submitRequest(authentication.getName(), dto);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", result,
                "message", result.getStatus().name().equals("ACCEPTED")
                        ? "Valet request accepted. You are within the service area."
                        : "Valet request rejected. Your location is outside the service area ("
                          + String.format("%.1f", result.getDistanceKm()) + " km away, "
                          + "limit is " + String.format("%.1f", result.getRadiusKm()) + " km).",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/valet/requests/mine — CUSTOMER views their own requests
    // -------------------------------------------------------------------------

    @GetMapping("/requests/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getMyRequests(
            Authentication authentication) {

        List<ValetRequestDto> requests = valetService.getMyRequests(authentication.getName());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", requests,
                "message", "Valet requests retrieved.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/valet/requests?locationId=... — CLERK / OWNER views branch requests
    // -------------------------------------------------------------------------

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('CLERK', 'OWNER')")
    public ResponseEntity<Map<String, Object>> getRequestsForLocation(
            @RequestParam UUID locationId) {

        List<ValetRequestDto> requests = valetService.getRequestsForLocation(locationId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", requests,
                "message", "Valet requests for branch retrieved.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/valet/requests/{id}/status — CLERK / OWNER advances the job
    // -------------------------------------------------------------------------

    /**
     * Update a valet request's status. Body: {@code {"status": "IN_PROGRESS"}}.
     * Invalid transitions return 422; unknown status values return 400.
     */
    @PatchMapping("/requests/{id}/status")
    @PreAuthorize("hasAnyRole('CLERK', 'OWNER')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String raw = body.get("status");
        ValetRequest.ValetStatus newStatus;
        try {
            newStatus = ValetRequest.ValetStatus.valueOf(raw == null ? "" : raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Unknown valet status: " + raw,
                    "timestamp", LocalDateTime.now().toString()
            ));
        }

        ValetRequestDto result = valetService.updateStatus(id, newStatus);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", result,
                "message", "Valet request updated to " + newStatus + ".",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/valet/requests/{id}/cancel — CUSTOMER cancels own request
    // -------------------------------------------------------------------------

    /** Cancel the customer's own valet request (PENDING or ACCEPTED only). */
    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> cancel(
            Authentication authentication,
            @PathVariable UUID id) {

        ValetRequestDto result = valetService.cancel(authentication.getName(), id);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", result,
                "message", "Valet request cancelled.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
