package com.carwash.backend.repository;

import com.carwash.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBooking_Id(UUID bookingId);

    List<Payment> findByBooking_IdIn(Collection<UUID> bookingIds);

    /** Sum of all COMPLETED payments whose createdAt falls within [start, end). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus = 'COMPLETED' " +
           "AND p.createdAt >= :start AND p.createdAt < :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /** Sum of COMPLETED payments for a specific method (CASH or TOYYIBPAY) within [start, end). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.paymentStatus = 'COMPLETED' AND p.method = :method " +
           "AND p.createdAt >= :start AND p.createdAt < :end")
    BigDecimal sumRevenueByMethodBetween(@Param("method") String method,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /** Count of payments with a given paymentStatus within [start, end). */
    @Query("SELECT COUNT(p) FROM Payment p " +
           "WHERE p.paymentStatus = :status " +
           "AND p.createdAt >= :start AND p.createdAt < :end")
    long countByPaymentStatusBetween(@Param("status") String status,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}
