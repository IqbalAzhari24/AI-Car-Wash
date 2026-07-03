package com.carwash.backend;

import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Review;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.ReviewRepository;
import com.carwash.backend.service.ReviewService;
import com.carwash.backend.service.SentimentAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookingRepository bookingRepository;
    @Mock SentimentAnalysisService sentimentAnalysisService;
    @InjectMocks ReviewService service;

    @Test
    void submit_throws_422_if_booking_not_completed() {
        UUID bookingId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        Booking booking = booking(bookingId, callerId, Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.submit(bookingId, callerId, 5, "great"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("only allowed after the wash is completed");
    }

    @Test
    void submit_throws_409_if_already_reviewed() {
        UUID bookingId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        Booking booking = booking(bookingId, callerId, Booking.BookingStatus.COMPLETED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBooking_Id(bookingId)).thenReturn(Optional.of(new Review()));

        assertThatThrownBy(() -> service.submit(bookingId, callerId, 5, "great"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Already reviewed");
    }

    @Test
    void submit_throws_400_if_rating_is_zero() {
        UUID bookingId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        assertThatThrownBy(() -> service.submit(bookingId, callerId, 0, "bad"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Rating must be 1");
    }

    @Test
    void submit_throws_400_if_rating_is_six() {
        UUID bookingId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        assertThatThrownBy(() -> service.submit(bookingId, callerId, 6, "bad"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Rating must be 1");
    }

    @Test
    void submit_throws_403_if_caller_is_not_booking_owner() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId   = UUID.randomUUID();
        UUID intruder  = UUID.randomUUID();

        Booking booking = booking(bookingId, ownerId, Booking.BookingStatus.COMPLETED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.submit(bookingId, intruder, 5, "great"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .hasToString("403 FORBIDDEN");
    }

    @Test
    void submit_succeeds_for_completed_booking_owner() {
        UUID bookingId = UUID.randomUUID();
        UUID callerId  = UUID.randomUUID();

        Booking booking = booking(bookingId, callerId, Booking.BookingStatus.COMPLETED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBooking_Id(bookingId)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        service.submit(bookingId, callerId, 4, "good wash");

        verify(reviewRepository).save(any(Review.class));
    }

    // --- helper ---

    private Booking booking(UUID bookingId, UUID customerId, Booking.BookingStatus status) {
        User customer = new User();
        customer.setId(customerId);
        customer.setRole(User.UserRole.CUSTOMER);

        Booking b = new Booking();
        b.setId(bookingId);
        b.setCustomer(customer);
        b.setStatus(status);
        return b;
    }
}
