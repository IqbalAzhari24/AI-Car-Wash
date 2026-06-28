package com.carwash.backend.service;

import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.util.SlotConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs once a day (01:00) and ensures the slot inventory always covers the next
 * 14 days. CatalogSeeder creates the initial window on first boot; this job
 * extends it so bookings never run out of available slots.
 *
 * Operating window and Friday-closed rule mirror CatalogSeeder exactly.
 */
@Component
public class SlotReplenishmentCron {

    private static final Logger log = LoggerFactory.getLogger(SlotReplenishmentCron.class);

    private final LocationRepository locationRepository;
    private final SlotCapacityRepository slotCapacityRepository;

    public SlotReplenishmentCron(LocationRepository locationRepository,
                                 SlotCapacityRepository slotCapacityRepository) {
        this.locationRepository = locationRepository;
        this.slotCapacityRepository = slotCapacityRepository;
    }

    @Scheduled(cron = "0 0 1 * * *") // 01:00 every day
    @Transactional
    public void replenish() {
        List<Location> locations = locationRepository.findActive();
        if (locations.isEmpty()) {
            log.warn("SlotReplenishmentCron: no active locations found, skipping.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(SlotConstants.DAYS_AHEAD);

        List<SlotCapacity> toCreate = new ArrayList<>();

        for (Location location : locations) {
            for (LocalDate day = today; !day.isAfter(horizon); day = day.plusDays(1)) {
                if (day.getDayOfWeek() == DayOfWeek.FRIDAY) {
                    continue; // closed on Fridays
                }
                for (LocalTime t = SlotConstants.OPEN; !t.isAfter(SlotConstants.LAST_SLOT); t = t.plusMinutes(SlotConstants.SLOT_MINUTES)) {
                    LocalDateTime slotTime = LocalDateTime.of(day, t);
                    if (slotCapacityRepository.findByLocationIdAndSlotTime(location.getId(), slotTime) == null) {
                        SlotCapacity slot = new SlotCapacity();
                        slot.setLocation(location);
                        slot.setSlotTime(slotTime);
                        slot.setMaxLimit(SlotConstants.MAX_PER_SLOT);
                        slot.setBookedCount(0);
                        toCreate.add(slot);
                    }
                }
            }
        }

        if (!toCreate.isEmpty()) {
            slotCapacityRepository.saveAll(toCreate);
            log.info("SlotReplenishmentCron: created {} new slots up to {}", toCreate.size(), horizon);
        } else {
            log.info("SlotReplenishmentCron: slot window is already full up to {}", horizon);
        }
    }
}
