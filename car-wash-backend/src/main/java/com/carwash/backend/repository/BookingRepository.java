package com.carwash.backend.repository;

import com.carwash.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByStatusAndSlotTimeBefore(Booking.BookingStatus status, LocalDateTime time);

    List<Booking> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    /** Active job queue for operators — bookings in the given statuses, earliest slot first. */
    List<Booking> findByStatusInOrderBySlotTimeAsc(Collection<Booking.BookingStatus> statuses);

    /** Count bookings with a given status whose createdAt falls within [start, end). */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.status = :status AND b.createdAt >= :start AND b.createdAt < :end")
    long countByBookingStatusBetween(@Param("status") Booking.BookingStatus status,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /** Count all bookings whose createdAt falls within [start, end). */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt >= :start AND b.createdAt < :end")
    long countAllBetween(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);
}