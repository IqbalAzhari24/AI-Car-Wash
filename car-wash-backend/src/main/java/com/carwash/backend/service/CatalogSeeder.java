package com.carwash.backend.service;

import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the bookable 30-minute slot inventory on first boot.
 * Location and services are seeded by V8__seed_catalog.sql (Flyway).
 *
 * Operating window: 09:00–17:30. Fridays are skipped (shop closed).
 */
@Component
@Order(20) // after UserSeeder
public class CatalogSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime LAST_SLOT = LocalTime.of(17, 30);
    private static final int SLOT_MINUTES = 30;
    private static final int DAYS_AHEAD = 14;
    private static final int MAX_PER_SLOT = 3;

    private final LocationRepository locationRepository;
    private final SlotCapacityRepository slotCapacityRepository;

    public CatalogSeeder(LocationRepository locationRepository,
                         SlotCapacityRepository slotCapacityRepository) {
        this.locationRepository = locationRepository;
        this.slotCapacityRepository = slotCapacityRepository;
    }

    @Override
    public void run(String... args) {
        if (slotCapacityRepository.count() > 0) {
            log.info("Slot inventory already present. Skipping slot seed.");
            return;
        }

        Location main = locationRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No location found — ensure V8__seed_catalog.sql has run."));

        List<SlotCapacity> slots = new ArrayList<>();
        LocalDate day = LocalDate.now();
        for (int d = 0; d < DAYS_AHEAD; d++, day = day.plusDays(1)) {
            if (day.getDayOfWeek() == DayOfWeek.FRIDAY) continue;
            for (LocalTime t = OPEN; !t.isAfter(LAST_SLOT); t = t.plusMinutes(SLOT_MINUTES)) {
                SlotCapacity slot = new SlotCapacity();
                slot.setLocation(main);
                slot.setSlotTime(LocalDateTime.of(day, t));
                slot.setMaxLimit(MAX_PER_SLOT);
                slot.setBookedCount(0);
                slots.add(slot);
            }
        }
        slotCapacityRepository.saveAll(slots);
        log.info("Seeded {} bookable slots for location '{}'.", slots.size(), main.getName());
    }
}
