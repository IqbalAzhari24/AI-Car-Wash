package com.carwash.backend.dto;

import com.carwash.backend.entity.SlotCapacity;

import java.time.LocalDateTime;

/**
 * Exposes slot availability for the booking wizard without leaking JPA entity internals.
 * Consumed by {@code GET /api/v1/slots?date=YYYY-MM-DD}.
 */
public class SlotAvailabilityDto {

    private LocalDateTime slotTime;
    private boolean available;
    private int bookedCount;
    private int maxLimit;

    /** Factory — maps a {@link SlotCapacity} entity to this DTO. */
    public static SlotAvailabilityDto from(SlotCapacity sc) {
        SlotAvailabilityDto dto = new SlotAvailabilityDto();
        dto.slotTime    = sc.getSlotTime();
        dto.available   = sc.getBookedCount() < sc.getMaxLimit();
        dto.bookedCount = sc.getBookedCount();
        dto.maxLimit    = sc.getMaxLimit();
        return dto;
    }

    public LocalDateTime getSlotTime()   { return slotTime;    }
    public boolean       isAvailable()   { return available;   }
    public int           getBookedCount(){ return bookedCount; }
    public int           getMaxLimit()   { return maxLimit;    }
}
