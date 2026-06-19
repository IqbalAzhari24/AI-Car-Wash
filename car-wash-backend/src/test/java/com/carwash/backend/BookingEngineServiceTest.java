package com.carwash.backend;

import com.carwash.backend.entity.Booking;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.service.BookingEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

// ponytail: pure unit test — no Spring context, runs in milliseconds
@ExtendWith(MockitoExtension.class)
class BookingEngineServiceTest {

    @Mock SlotCapacityRepository slotCapacityRepository;
    @InjectMocks BookingEngineService engine;

    // --- calculateRequiredBlocks ---

    @Test
    void motorcycle_needs_one_block() {
        assertThat(engine.calculateRequiredBlocks(Booking.VehicleClass.MOTORCYCLE)).isEqualTo(1);
    }

    @Test
    void sedan_needs_one_block() {
        assertThat(engine.calculateRequiredBlocks(Booking.VehicleClass.SEDAN)).isEqualTo(1);
    }

    @Test
    void suv_luxury_needs_two_blocks() {
        assertThat(engine.calculateRequiredBlocks(Booking.VehicleClass.SUV_LUXURY)).isEqualTo(2);
    }

    @Test
    void mpv_large_needs_three_blocks() {
        assertThat(engine.calculateRequiredBlocks(Booking.VehicleClass.MPV_LARGE)).isEqualTo(3);
    }

}
