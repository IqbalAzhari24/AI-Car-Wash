package com.carwash.backend.service;

import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.SlotCapacityRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class BookingEngineService {

    private final SlotCapacityRepository slotCapacityRepository;

    public BookingEngineService(SlotCapacityRepository slotCapacityRepository) {
        this.slotCapacityRepository = slotCapacityRepository;
    }

    /**
     * Executes transactional safety checks.
     * Looks ahead sequentially to confirm contiguous blocks are available.
     * 1 block = 30 minutes.
     */
    public boolean validateConsecutiveSlots(UUID locationId, LocalDateTime startTime, Booking.VehicleClass vClass) {
        int requiredBlocks = calculateRequiredBlocks(vClass);
        LocalDateTime checkTime = startTime;
        
        for (int i = 0; i < requiredBlocks; i++) {
            SlotCapacity capacity = slotCapacityRepository.findByLocationIdAndSlotTime(locationId, checkTime);
            if (capacity == null || capacity.getBookedCount() >= capacity.getMaxLimit()) {
                return false;
            }
            checkTime = checkTime.plusMinutes(30);
        }
        return true;
    }

    public int calculateRequiredBlocks(Booking.VehicleClass vClass) {
        switch (vClass) {
            case SUV_LUXURY:
                return 2; // 60 mins total
            case MPV_LARGE:
                return 3; // 90 mins total
            case MOTORCYCLE:
            case COMPACT:
            case SEDAN:
            default:
                return 1; // 30 mins total
        }
    }

    /**
     * Applies the financial pricing matrix dynamically based on vehicle size.
     */
    public java.math.BigDecimal calculatePrice(java.math.BigDecimal basePrice, Booking.VehicleClass vClass) {
        switch (vClass) {
            case MOTORCYCLE:
                return basePrice.subtract(new java.math.BigDecimal("10.00"));
            case COMPACT:
                return basePrice.subtract(new java.math.BigDecimal("5.00"));
            case SUV_LUXURY:
                return basePrice.add(new java.math.BigDecimal("15.00"));
            case MPV_LARGE:
                return basePrice.add(new java.math.BigDecimal("25.00"));
            case SEDAN:
            default:
                return basePrice;
        }
    }
}
