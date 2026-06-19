package com.carwash.backend.service;

import com.carwash.backend.dto.RevenueTrendDto;
import com.carwash.backend.dto.SalesSummaryDto;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Analytics service for the Owner dashboard.
 * All revenue figures reflect COMPLETED payments only (money actually received).
 */
@Service
public class OwnerAnalyticsService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public OwnerAnalyticsService(PaymentRepository paymentRepository,
                                 BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Returns a daily sales snapshot for the given date.
     *
     * @param date the calendar date to summarise (Malaysia local time)
     * @return {@link SalesSummaryDto} containing revenue and booking counts
     */
    @Transactional(readOnly = true)
    public SalesSummaryDto getSummary(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.plusDays(1).atStartOfDay();

        SalesSummaryDto dto = new SalesSummaryDto();
        dto.setDate(date);

        // Revenue
        dto.setTotalRevenue(paymentRepository.sumRevenueBetween(start, end));
        dto.setCashRevenue(paymentRepository.sumRevenueByMethodBetween("CASH", start, end));
        dto.setOnlineRevenue(paymentRepository.sumRevenueByMethodBetween("TOYYIBPAY", start, end));

        // Payment counts
        dto.setCompletedPayments(paymentRepository.countByPaymentStatusBetween("COMPLETED", start, end));
        dto.setPendingPayments(paymentRepository.countByPaymentStatusBetween("PENDING", start, end));
        dto.setFailedPayments(paymentRepository.countByPaymentStatusBetween("FAILED", start, end));

        // Booking counts
        dto.setTotalBookings(bookingRepository.countAllBetween(start, end));
        dto.setCompletedBookings(bookingRepository.countByBookingStatusBetween(Booking.BookingStatus.COMPLETED, start, end));
        dto.setConfirmedBookings(bookingRepository.countByBookingStatusBetween(Booking.BookingStatus.CONFIRMED, start, end));
        dto.setPendingBookings(bookingRepository.countByBookingStatusBetween(Booking.BookingStatus.PENDING, start, end));
        dto.setCancelledBookings(bookingRepository.countByBookingStatusBetween(Booking.BookingStatus.CANCELLED, start, end));
        dto.setNoShowBookings(bookingRepository.countByBookingStatusBetween(Booking.BookingStatus.NO_SHOW, start, end));

        return dto;
    }

    /**
     * Returns one revenue trend point per day for the last {@code days} days
     * (today inclusive), ordered chronologically oldest-first.
     *
     * @param days number of days to look back (1–90; clamped at 90)
     * @return ordered list of {@link RevenueTrendDto}
     */
    @Transactional(readOnly = true)
    public List<RevenueTrendDto> getTrend(int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(safeDays - 1);

        List<RevenueTrendDto> trend = new ArrayList<>(safeDays);
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end   = d.plusDays(1).atStartOfDay();

            BigDecimal revenue     = paymentRepository.sumRevenueBetween(start, end);
            long       bookings    = bookingRepository.countAllBetween(start, end);
            trend.add(new RevenueTrendDto(d, revenue, bookings));
        }
        return trend;
    }
}
