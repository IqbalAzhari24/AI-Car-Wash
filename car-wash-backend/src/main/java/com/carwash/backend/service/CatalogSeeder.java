package com.carwash.backend.service;

import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.Service;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.ServiceRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the operational catalogue (a default branch, the wash services, and the
 * bookable 30-minute slot inventory) on first boot so the booking engine has
 * something to reserve against. Idempotent: skips if any location already exists.
 *
 * Operating window: 09:00–17:30 (last 30-min start ends by 18:00 close). Fridays
 * are seeded with no slots, which is how the "closed on Fridays" rule is enforced
 * as data rather than hardcoded logic.
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
    private final ServiceRepository serviceRepository;
    private final SlotCapacityRepository slotCapacityRepository;

    public CatalogSeeder(LocationRepository locationRepository,
                         ServiceRepository serviceRepository,
                         SlotCapacityRepository slotCapacityRepository) {
        this.locationRepository = locationRepository;
        this.serviceRepository = serviceRepository;
        this.slotCapacityRepository = slotCapacityRepository;
    }

    @Override
    public void run(String... args) {
        if (locationRepository.count() > 0) {
            log.info("Catalogue already present. Skipping catalogue seed.");
            return;
        }
        log.info("Seeding default location, services and slot inventory...");

        Location main = new Location();
        main.setName("Timah Wash - Main");
        main.setAddress("Jalan Utama, Kuala Lumpur");
        main.setActive(true);
        main = locationRepository.save(main);

        serviceRepository.saveAll(List.of(
                service("Standard Wash", "Exterior wash and dry", "25.00", 30),
                service("Premium Wash", "Exterior + interior vacuum and wipe-down", "45.00", 45),
                service("Full Detailing", "Deep clean, polish and wax", "120.00", 90)
        ));

        List<SlotCapacity> slots = new ArrayList<>();
        LocalDate day = LocalDate.now();
        for (int d = 0; d < DAYS_AHEAD; d++, day = day.plusDays(1)) {
            if (day.getDayOfWeek() == DayOfWeek.FRIDAY) {
                continue; // closed on Fridays
            }
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
        log.info("Seeded 1 location, 3 services and {} bookable slots.", slots.size());
    }

    private Service service(String name, String description, String price, int minutes) {
        Service s = new Service();
        s.setName(name);
        s.setDescription(description);
        s.setPrice(new BigDecimal(price));
        s.setDurationMinutes(minutes);
        s.setActive(true);
        return s;
    }
}
