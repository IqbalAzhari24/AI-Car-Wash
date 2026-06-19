package com.carwash.backend.repository;

import com.carwash.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByBooking_Id(UUID bookingId);

    List<Review> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    // idx_reviews_sentiment supports ordering/filtering by AI-computed sentiment
    @Query("SELECT r FROM Review r WHERE r.sentimentScore IS NOT NULL ORDER BY r.sentimentScore ASC")
    List<Review> findOrderedBySentiment();
}
