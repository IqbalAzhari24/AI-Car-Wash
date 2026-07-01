package com.carwash.backend.service;

import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.util.MalaysiaCalendarService;
import com.carwash.backend.util.SlotConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
 * <p>Operating-day logic is delegated to {@link MalaysiaCalendarService}, which
 * handles both the Friday prayer window and Malaysian public holidays (fixed and
 * dynamic Islamic dates).
 */
@Component
public class SlotReplenishmentCron {

    private static final Logger log = LoggerFactory.getLogger(SlotReplenishmentCron.class);

    private final LocationRepository locationRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final MalaysiaCalendarService malaysiaCalendarService;

    public SlotReplenishmentCron(LocationRepository locationRepository,
                                 SlotCapacityRepository slotCapacityRepository,
                                 MalaysiaCalendarService malaysiaCalendarService) {
        this.locationRepository = locationRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.malaysiaCalendarService = malaysiaCalendarService;
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
                // Skip Fridays and Malaysian public holidays
                if (!malaysiaCalendarService.isOperatingDay(day)) {
                    log.debug("SlotReplenishmentCron: skipping {} (non-operating day)", day);
                    continue;
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
