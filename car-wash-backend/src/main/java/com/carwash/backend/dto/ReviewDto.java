package com.carwash.backend.dto;

import com.carwash.backend.entity.Review;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReviewDto {
    private final UUID id;
    private final UUID bookingId;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;

    public ReviewDto(UUID id, UUID bookingId, int rating, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public static ReviewDto from(Review r) {
        return new ReviewDto(
                r.getId(),
                r.getBooking().getId(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt());
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
