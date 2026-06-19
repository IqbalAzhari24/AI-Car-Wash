package com.carwash.backend.service;

import com.carwash.backend.entity.Booking;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class NoShowAutomationCron {

    private static final Logger log = LoggerFactory.getLogger(NoShowAutomationCron.class);
    private final BookingRepository bookingRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final BookingEngineService bookingEngineService;

    public NoShowAutomationCron(BookingRepository bookingRepository, 
                                SlotCapacityRepository slotCapacityRepository,
                                BookingEngineService bookingEngineService) {
        this.bookingRepository = bookingRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.bookingEngineService = bookingEngineService;
    }

    /**
     * Runs every 300 seconds (5 minutes).
     * Clears confirmed slots that are 15 minutes past slot_time.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processNoShows() {
        log.info("Running NoShow Automation Cron");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Booking> overdueBookings = bookingRepository.findByStatusAndSlotTimeBefore(Booking.BookingStatus.CONFIRMED, threshold);
        
        for (Booking booking : overdueBookings) {
            booking.setStatus(Booking.BookingStatus.NO_SHOW);

            int blocks = bookingEngineService.calculateRequiredBlocks(booking.getVClass());
            LocalDateTime currentTime = booking.getSlotTime();
            for (int i = 0; i < blocks; i++) {
                slotCapacityRepository.decrementBookedCount(booking.getLocation().getId(), currentTime);
                currentTime = currentTime.plusMinutes(30);
            }
            log.info("Marked booking {} as NO_SHOW and freed {} slots", booking.getId(), blocks);
        }
        if (!overdueBookings.isEmpty()) {
            bookingRepository.saveAll(overdueBookings);
        }
    }
}
