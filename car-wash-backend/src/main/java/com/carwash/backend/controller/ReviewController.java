package com.carwash.backend.controller;

import com.carwash.backend.dto.ReviewDto;
import com.carwash.backend.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{id}/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** GET — 200 with body if reviewed, 204 if not yet. */
    @GetMapping
    public ResponseEntity<ReviewDto> get(@PathVariable UUID id, Authentication auth) {
        return reviewService.findByBooking(id, UUID.fromString(auth.getName()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** POST — submit a review. 201 on success, 422 if not COMPLETED, 409 if duplicate. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto submit(@PathVariable UUID id,
                            @RequestBody Map<String, Object> body,
                            Authentication auth) {
        int rating = Integer.parseInt(String.valueOf(body.get("rating")));
        String comment = body.containsKey("comment") ? String.valueOf(body.get("comment")) : null;
        return reviewService.submit(id, UUID.fromString(auth.getName()), rating, comment);
    }
}
