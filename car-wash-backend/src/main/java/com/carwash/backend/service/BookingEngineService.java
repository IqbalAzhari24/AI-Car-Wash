package com.carwash.backend.service;

import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Service;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class BookingEngineService {

    public int calculateRequiredBlocks(Booking.VehicleClass vClass) {
        return switch (vClass) {
            case SUV_LUXURY -> 2; // 60 mins total
            case MPV_LARGE  -> 3; // 90 mins total
            default         -> 1; // 30 mins total
        };
    }

    /**
     * Vehicle-class price multiplier applied on top of {@code service.getPrice()}.
     *
     * [DESIGN DECISION - please review]
     * These ratios are derived from the previous flat +/-RM matrix (which assumed a
     * fixed RM25 base price): MOTORCYCLE -10 -> 15/25 = 0.60, COMPACT -5 -> 20/25 = 0.80,
     * SEDAN +0 -> 1.00, SUV_LUXURY +15 -> 40/25 = 1.60, MPV_LARGE +25 -> 50/25 = 2.00.
     * Expressing the old deltas as ratios means the price now scales with each
     * service's own catalogue price instead of a single global base price.
     * Adjust these constants if a different pricing policy is wanted.
     */
    private static final Map<Booking.VehicleClass, BigDecimal> VEHICLE_CLASS_MULTIPLIER = Map.of(
            Booking.VehicleClass.MOTORCYCLE, new BigDecimal("0.60"),
            Booking.VehicleClass.COMPACT, new BigDecimal("0.80"),
            Booking.VehicleClass.SEDAN, BigDecimal.ONE,
            Booking.VehicleClass.SUV_LUXURY, new BigDecimal("1.60"),
            Booking.VehicleClass.MPV_LARGE, new BigDecimal("2.00")
    );

    /**
     * Computes the booking price from the selected service's catalogue price,
     * scaled by the vehicle-class multiplier and the service's own
     * vehicle_size_multiplier (e.g. detailing a large van costs more than a
     * motorcycle, and some services scale more steeply with vehicle size than
     * others).
     */
    public BigDecimal calculatePrice(Service service, Booking.VehicleClass vClass) {
        BigDecimal classMultiplier = VEHICLE_CLASS_MULTIPLIER.getOrDefault(vClass, BigDecimal.ONE);
        BigDecimal sizeMultiplier = service.getVehicleSizeMultiplier() != null
                ? service.getVehicleSizeMultiplier()
                : BigDecimal.ONE;
        return service.getPrice()
                .multiply(classMultiplier)
                .multiply(sizeMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
