package com.carwash.backend;

import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SlotCapacityRepositoryTest extends AbstractIntegrationTest {

    @Autowired SlotCapacityRepository slotCapacityRepository;
    @Autowired LocationRepository locationRepository;

    Location location;
    LocalDateTime slotTime;

    @BeforeEach
    void setup() {
        // Own location per test — never deleteAll(): the V15 demo-data migration
        // seeds bookings whose FK to locations makes a global wipe fail (23503).
        location = new Location();
        location.setName("Test Branch");
        location.setAddress("1 Test St");
        location = locationRepository.save(location);

        slotTime = LocalDateTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0);
    }

    @AfterEach
    void cleanup() {
        SlotCapacity own = slotCapacityRepository.findByLocationIdAndSlotTime(location.getId(), slotTime);
        if (own != null) slotCapacityRepository.delete(own);
        locationRepository.delete(location);
    }

    @Test
    void incrementBookedCount_returns_1_when_capacity_available() {
        SlotCapacity sc = slot(3, 0);
        slotCapacityRepository.save(sc);

        int rows = slotCapacityRepository.incrementBookedCount(location.getId(), slotTime);
        assertThat(rows).isEqualTo(1);

        SlotCapacity updated = slotCapacityRepository.findByLocationIdAndSlotTime(location.getId(), slotTime);
        assertThat(updated.getBookedCount()).isEqualTo(1);
    }

    @Test
    void incrementBookedCount_returns_0_when_slot_is_full() {
        SlotCapacity sc = slot(3, 3); // bookedCount == maxLimit
        slotCapacityRepository.save(sc);

        int rows = slotCapacityRepository.incrementBookedCount(location.getId(), slotTime);
        assertThat(rows).isEqualTo(0);

        SlotCapacity unchanged = slotCapacityRepository.findByLocationIdAndSlotTime(location.getId(), slotTime);
        assertThat(unchanged.getBookedCount()).isEqualTo(3);
    }

    @Test
    void decrementBookedCount_does_not_go_below_zero() {
        SlotCapacity sc = slot(3, 0); // already at 0
        slotCapacityRepository.save(sc);

        int rows = slotCapacityRepository.decrementBookedCount(location.getId(), slotTime);
        assertThat(rows).isEqualTo(0);

        SlotCapacity unchanged = slotCapacityRepository.findByLocationIdAndSlotTime(location.getId(), slotTime);
        assertThat(unchanged.getBookedCount()).isEqualTo(0);
    }

    @Test
    void decrementBookedCount_returns_1_when_count_positive() {
        SlotCapacity sc = slot(3, 2);
        slotCapacityRepository.save(sc);

        int rows = slotCapacityRepository.decrementBookedCount(location.getId(), slotTime);
        assertThat(rows).isEqualTo(1);

        SlotCapacity updated = slotCapacityRepository.findByLocationIdAndSlotTime(location.getId(), slotTime);
        assertThat(updated.getBookedCount()).isEqualTo(1);
    }

    private SlotCapacity slot(int maxLimit, int bookedCount) {
        SlotCapacity sc = new SlotCapacity();
        sc.setLocation(location);
        sc.setSlotTime(slotTime);
        sc.setMaxLimit(maxLimit);
        sc.setBookedCount(bookedCount);
        return sc;
    }
}
