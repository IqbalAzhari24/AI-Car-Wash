package com.carwash.backend.repository;

import com.carwash.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByBooking_Id(UUID bookingId);
}
