package com.carwash.backend.repository;

import com.carwash.backend.entity.SlotCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SlotCapacityRepository extends JpaRepository<SlotCapacity, UUID> {
    
    SlotCapacity findByLocationIdAndSlotTime(UUID locationId, LocalDateTime slotTime);

    /**
     * Optimized: Fetches only the required slot capacity windows to eliminate table sweeps.
     */
    List<SlotCapacity> findBySlotTimeBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("UPDATE SlotCapacity s SET s.bookedCount = s.bookedCount - 1 WHERE s.location.id = :locationId AND s.slotTime = :slotTime AND s.bookedCount > 0")
    int decrementBookedCount(@Param("locationId") UUID locationId, @Param("slotTime") LocalDateTime slotTime);

    /**
     * Atomically reserves one unit of capacity for a slot. The {@code bookedCount < maxLimit}
     * guard means concurrent callers cannot oversubscribe: only the updates that actually
     * change a row (return 1) win the slot; a return of 0 means the slot is full (or missing).
     */
    @Modifying
    @Query("UPDATE SlotCapacity s SET s.bookedCount = s.bookedCount + 1 WHERE s.location.id = :locationId AND s.slotTime = :slotTime AND s.bookedCount < s.maxLimit")
    int incrementBookedCount(@Param("locationId") UUID locationId, @Param("slotTime") LocalDateTime slotTime);
}