package com.carwash.backend;

import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.service.BookingEngineService;
import com.carwash.backend.service.NoShowAutomationCron;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoShowAutomationCronTest {

    @Mock BookingRepository bookingRepository;
    @Mock SlotCapacityRepository slotCapacityRepository;
    @Mock BookingEngineService bookingEngineService;

    @InjectMocks NoShowAutomationCron cron;

    // -------------------------------------------------------------------------
    // Happy path: single overdue CONFIRMED booking → marked NO_SHOW
    // -------------------------------------------------------------------------

    @Test
    void processNoShows_marks_overdue_confirmed_booking_as_no_show() {
        Booking booking = confirmedBooking(Booking.VehicleClass.SEDAN,
                LocalDateTime.now().minusMinutes(30));

        when(bookingRepository.findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingEngineService.calculateRequiredBlocks(Booking.VehicleClass.SEDAN))
                .thenReturn(1);

        cron.processNoShows();

        assertThat(booking.getStatus()).isEqualTo(Booking.BookingStatus.NO_SHOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Booking>> saved = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(booking);
    }

    // -------------------------------------------------------------------------
    // No overdue bookings → saveAll is never called
    // -------------------------------------------------------------------------

    @Test
    void processNoShows_does_nothing_when_no_overdue_bookings() {
        when(bookingRepository.findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        cron.processNoShows();

        verify(bookingRepository, never()).saveAll(any());
        verify(slotCapacityRepository, never()).decrementBookedCount(any(), any());
    }

    // -------------------------------------------------------------------------
    // SEDAN → 1 block released
    // -------------------------------------------------------------------------

    @Test
    void processNoShows_frees_one_slot_block_for_sedan() {
        UUID locationId = UUID.randomUUID();
        LocalDateTime slotTime = LocalDateTime.now().minusMinutes(30);
        Booking booking = confirmedBookingForLocation(locationId, Booking.VehicleClass.SEDAN, slotTime);

        when(bookingRepository.findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingEngineService.calculateRequiredBlocks(Booking.VehicleClass.SEDAN))
                .thenReturn(1);

        cron.processNoShows();

        verify(slotCapacityRepository, times(1))
                .decrementBookedCount(locationId, slotTime);
    }

    // -------------------------------------------------------------------------
    // SUV_LUXURY → 2 blocks released at slotTime and slotTime+30min
    // -------------------------------------------------------------------------

    @Test
    void processNoShows_frees_two_slot_blocks_for_suv_luxury() {
        UUID locationId = UUID.randomUUID();
        LocalDateTime slotTime = LocalDateTime.now().minusMinutes(60);
        Booking booking = confirmedBookingForLocation(locationId, Booking.VehicleClass.SUV_LUXURY, slotTime);

        when(bookingRepository.findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingEngineService.calculateRequiredBlocks(Booking.VehicleClass.SUV_LUXURY))
                .thenReturn(2);

        cron.processNoShows();

        verify(slotCapacityRepository).decrementBookedCount(locationId, slotTime);
        verify(slotCapacityRepository).decrementBookedCount(locationId, slotTime.plusMinutes(30));
        verify(slotCapacityRepository, times(2))
                .decrementBookedCount(eq(locationId), any(LocalDateTime.class));
    }

    // -------------------------------------------------------------------------
    // MPV_LARGE → 3 blocks released
    // -------------------------------------------------------------------------

    @Test
    void processNoShows_frees_three_slot_blocks_for_mpv_large() {
        UUID locationId = UUID.randomUUID();
        LocalDateTime slotTime = LocalDateTime.now().minusMinutes(90);
        Booking booking = confirmedBookingForLocation(locationId, Booking.VehicleClass.MPV_LARGE, slotTime);

        when(bookingRepository.findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingEngineService.calculateRequiredBlocks(Booking.VehicleClass.MPV_LARGE))
                .thenReturn(3);

        cron.processNoShows();

        verify(slotCapacityRepository).decrementBookedCount(locationId, slotTime);
        verify(slotCapacityRepository).decrementBookedCount(locationId, slotTime.plusMinutes(30));
        verify(slotCapacityRepository).decrementBookedCount(locationId, slotTime.plusMinutes(60));
        verify(slotCapacityRepository, times(3))
                .decrementBookedCount(eq(locationId), any(LocalDateTime.class));
    }

    // -------------------------------------------------------------------------
    // Multiple overdue bookings → all saved in one saveAll call
    // -------------------------------------------------------------------------

    @Test
    void processNoShows_saves_all_updated_bookings_in_one_call() {
        Booking b1 = confirmedBooking(Booking.VehicleClass.SEDAN, LocalDateTime.now().minusMinutes(20));
        Booking b2 = confirmedBooking(Booking.VehicleClass.COMPACT, LocalDateTime.now().minusMinutes(25));

        when(bookingRepository.findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(b1, b2));
        when(bookingEngineService.calculateRequiredBlocks(Booking.VehicleClass.SEDAN)).thenReturn(1);
        when(bookingEngineService.calculateRequiredBlocks(Booking.VehicleClass.COMPACT)).thenReturn(1);

        cron.processNoShows();

        assertThat(b1.getStatus()).isEqualTo(Booking.BookingStatus.NO_SHOW);
        assertThat(b2.getStatus()).isEqualTo(Booking.BookingStatus.NO_SHOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Booking>> saved = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository, times(1)).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactlyInAnyOrder(b1, b2);
    }

    // -------------------------------------------------------------------------
    // Threshold: query is called with a time roughly 15min in the past
    // -------------------------------------------------------------------------

    @Test
    void processNoShows_queries_with_threshold_15_minutes_ago() {
        when(bookingRepository.findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusMinutes(15);
        cron.processNoShows();
        LocalDateTime after = LocalDateTime.now().minusMinutes(15);

        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingRepository).findByStatusAndSlotTimeBefore(
                eq(Booking.BookingStatus.CONFIRMED), thresholdCaptor.capture());

        LocalDateTime captured = thresholdCaptor.getValue();
        // Allow 2s clock skew in the test environment
        assertThat(captured).isBetween(before.minusSeconds(2), after.plusSeconds(2));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Booking confirmedBooking(Booking.VehicleClass vClass, LocalDateTime slotTime) {
        return confirmedBookingForLocation(UUID.randomUUID(), vClass, slotTime);
    }

    private Booking confirmedBookingForLocation(UUID locationId,
                                                Booking.VehicleClass vClass,
                                                LocalDateTime slotTime) {
        User customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setEmail("customer@test.com");
        customer.setRole(User.UserRole.CUSTOMER);

        Location location = new Location();
        location.setId(locationId);
        location.setName("Test Branch");

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setCustomer(customer);
        booking.setLocation(location);
        booking.setSlotTime(slotTime);
        booking.setVClass(vClass);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setVehicleModel("Test Car");
        booking.setTotalPrice(BigDecimal.valueOf(50.00));
        booking.setOverride(false);
        return booking;
    }
}
