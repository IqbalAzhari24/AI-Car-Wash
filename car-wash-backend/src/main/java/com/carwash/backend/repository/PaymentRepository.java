package com.carwash.backend.repository;

import com.carwash.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBooking_Id(UUID bookingId);

    List<Payment> findByBooking_IdIn(Collection<UUID> bookingIds);
}
