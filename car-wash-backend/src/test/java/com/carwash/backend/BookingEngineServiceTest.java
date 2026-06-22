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

    // --- calculatePrice ---

    private com.carwash.backend.entity.Service service(double price) {
        com.carwash.backend.entity.Service s = new com.carwash.backend.entity.Service();
        s.setPrice(new java.math.BigDecimal(price).setScale(2, java.math.RoundingMode.HALF_UP));
        s.setVehicleSizeMultiplier(java.math.BigDecimal.ONE);
        return s;
    }

    @Test
    void calculatePrice_sedan_is_base_price() {
        var s = service(25.0);
        assertThat(engine.calculatePrice(s, Booking.VehicleClass.SEDAN))
                .isEqualByComparingTo("25.00");
    }

    @Test
    void calculatePrice_motorcycle_applies_0_60_multiplier() {
        var s = service(25.0);
        assertThat(engine.calculatePrice(s, Booking.VehicleClass.MOTORCYCLE))
                .isEqualByComparingTo("15.00");
    }

    @Test
    void calculatePrice_compact_applies_0_80_multiplier() {
        var s = service(25.0);
        assertThat(engine.calculatePrice(s, Booking.VehicleClass.COMPACT))
                .isEqualByComparingTo("20.00");
    }

    @Test
    void calculatePrice_suv_luxury_applies_1_60_multiplier() {
        var s = service(25.0);
        assertThat(engine.calculatePrice(s, Booking.VehicleClass.SUV_LUXURY))
                .isEqualByComparingTo("40.00");
    }

    @Test
    void calculatePrice_mpv_large_applies_2_00_multiplier() {
        var s = service(25.0);
        assertThat(engine.calculatePrice(s, Booking.VehicleClass.MPV_LARGE))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void calculatePrice_applies_vehicle_size_multiplier_from_service() {
        var s = service(100.0);
        s.setVehicleSizeMultiplier(new java.math.BigDecimal("1.50"));
        // SEDAN (x1.00) * serviceMultiplier (x1.50) = 150.00
        assertThat(engine.calculatePrice(s, Booking.VehicleClass.SEDAN))
                .isEqualByComparingTo("150.00");
    }

    @Test
    void calculatePrice_rounds_half_up_to_2_decimals() {
        var s = service(10.0);
        // MOTORCYCLE (x0.60): 10 * 0.60 = 6.00 — use a price that creates a rounding case
        s.setPrice(new java.math.BigDecimal("10.01"));
        // MOTORCYCLE: 10.01 * 0.60 = 6.006 → rounds to 6.01
        assertThat(engine.calculatePrice(s, Booking.VehicleClass.MOTORCYCLE))
                .isEqualByComparingTo("6.01");
    }

}
