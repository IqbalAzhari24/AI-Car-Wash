package com.carwash.backend.service;

import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.util.MalaysiaCalendarService;
import com.carwash.backend.util.SlotConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the bookable 30-minute slot inventory on first boot.
 * Location and services are seeded by V8__seed_catalog.sql (Flyway).
 *
 * Operating window: 09:00–17:30. Fridays and Malaysian public holidays are skipped.
 */
@Component
@Order(20) // after UserSeeder
public class CatalogSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private final LocationRepository locationRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final MalaysiaCalendarService malaysiaCalendarService;

    public CatalogSeeder(LocationRepository locationRepository,
                         SlotCapacityRepository slotCapacityRepository,
                         MalaysiaCalendarService malaysiaCalendarService) {
        this.locationRepository = locationRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.malaysiaCalendarService = malaysiaCalendarService;
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
        for (int d = 0; d < SlotConstants.DAYS_AHEAD; d++, day = day.plusDays(1)) {
            if (!malaysiaCalendarService.isOperatingDay(day)) continue;
            for (LocalTime t = SlotConstants.OPEN; !t.isAfter(SlotConstants.LAST_SLOT); t = t.plusMinutes(SlotConstants.SLOT_MINUTES)) {
                SlotCapacity slot = new SlotCapacity();
                slot.setLocation(main);
                slot.setSlotTime(LocalDateTime.of(day, t));
                slot.setMaxLimit(SlotConstants.MAX_PER_SLOT);
                slot.setBookedCount(0);
                slots.add(slot);
            }
        }
        slotCapacityRepository.saveAll(slots);
        log.info("Seeded {} bookable slots for location '{}'.", slots.size(), main.getName());
    }
}
