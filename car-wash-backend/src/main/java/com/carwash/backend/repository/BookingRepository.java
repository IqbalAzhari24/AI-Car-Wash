package com.carwash.backend.repository;

import com.carwash.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByStatusAndSlotTimeBefore(Booking.BookingStatus status, LocalDateTime time);

    List<Booking> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
}