package com.carwash.backend.service;

import com.carwash.backend.dto.AnalyticsInsightDto;
import com.carwash.backend.dto.RevenueTrendDto;
import com.carwash.backend.dto.ReviewInsightDto;
import com.carwash.backend.dto.SalesSummaryDto;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Review;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.PaymentRepository;
import com.carwash.backend.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Analytics service for the Owner dashboard.
 * All revenue figures reflect COMPLETED payments only (money actually received).
 */
@Service
public class OwnerAnalyticsService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final MalaysiaCalendarService malaysiaCalendarService;

    public OwnerAnalyticsService(PaymentRepository paymentRepository,
                                 BookingRepository bookingRepository,
                                 ReviewRepository reviewRepository,
                                 MalaysiaCalendarService malaysiaCalendarService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.malaysiaCalendarService = malaysiaCalendarService;
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

    /**
     * Rule-based descriptive summary for the given date: revenue/bookings vs the
     * trailing 7-day average, plus Malaysia public holiday context (today, and any
     * holiday landing in the next 7 days that could shift near-term demand).
     */
    @Transactional(readOnly = true)
    public AnalyticsInsightDto getInsight(LocalDate date) {
        SalesSummaryDto today = getSummary(date);

        List<RevenueTrendDto> priorDays = getTrend(8).stream()
                .filter(t -> t.getDate().isBefore(date))
                .collect(Collectors.toList());

        BigDecimal avgRevenue = BigDecimal.ZERO;
        double avgBookings = 0;
        if (!priorDays.isEmpty()) {
            BigDecimal sum = priorDays.stream()
                    .map(RevenueTrendDto::getRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgRevenue = sum.divide(BigDecimal.valueOf(priorDays.size()), 2, RoundingMode.HALF_UP);
            avgBookings = priorDays.stream().mapToLong(RevenueTrendDto::getBookingCount).average().orElse(0);
        }

        StringBuilder sb = new StringBuilder();

        if (avgRevenue.compareTo(BigDecimal.ZERO) > 0) {
            double pctChange = today.getTotalRevenue().subtract(avgRevenue)
                    .divide(avgRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100;
            String direction = pctChange > 5 ? "up" : pctChange < -5 ? "down" : "in line with";
            sb.append(String.format("Revenue today (RM %.2f) is %s the 7-day average (RM %.2f)",
                    today.getTotalRevenue(), direction, avgRevenue));
            if (Math.abs(pctChange) > 5) {
                sb.append(String.format(" — %s%.0f%%", pctChange > 0 ? "+" : "", pctChange));
            }
            sb.append(". ");
        } else {
            sb.append(String.format("Revenue today: RM %.2f. ", today.getTotalRevenue()));
        }

        sb.append(today.getTotalBookings()).append(" booking")
          .append(today.getTotalBookings() == 1 ? "" : "s").append(" today");
        if (avgBookings > 0) {
            sb.append(String.format(" (7-day average: %.1f).", avgBookings));
        } else {
            sb.append(".");
        }

        Optional<String> holidayToday = malaysiaCalendarService.getHolidayName(date);
        holidayToday.ifPresent(name -> sb.append(" Today is ").append(name)
                .append(" — a public holiday, which may explain a shift from the usual pattern."));

        List<MalaysiaCalendarService.HolidayEntry> upcoming =
                malaysiaCalendarService.getUpcoming(date.plusDays(1), 7);
        if (!upcoming.isEmpty()) {
            String names = upcoming.stream()
                    .map(h -> h.name() + " (" + h.date() + ")")
                    .collect(Collectors.joining(", "));
            sb.append(" Coming up in the next 7 days: ").append(names)
              .append(" — expect demand to shift around then.");
        }

        List<AnalyticsInsightDto.UpcomingHoliday> upcomingDtos = upcoming.stream()
                .map(h -> new AnalyticsInsightDto.UpcomingHoliday(h.date(), h.name()))
                .collect(Collectors.toList());

        return new AnalyticsInsightDto(date, sb.toString().trim(),
                holidayToday.isPresent(), holidayToday.orElse(null), upcomingDtos);
    }

    /**
     * Aggregates customer reviews into a sentiment insight: average rating,
     * average AI sentiment score, positive/neutral/negative distribution
     * (thresholds ±0.2), a human-readable summary, and the three most negative
     * reviews for follow-up.
     *
     * @return {@link ReviewInsightDto} for the owner dashboard
     */
    @Transactional(readOnly = true)
    public ReviewInsightDto getReviewInsight() {
        // ponytail: full-table scan is fine at FYP scale; add a date-window query if reviews grow large
        List<Review> reviews = reviewRepository.findAll();

        if (reviews.isEmpty()) {
            return new ReviewInsightDto(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0,
                    "No customer reviews yet.", List.of());
        }

        BigDecimal avgRating = BigDecimal.valueOf(
                reviews.stream().mapToInt(Review::getRating).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);

        List<Review> scored = reviews.stream()
                .filter(r -> r.getSentimentScore() != null)
                .collect(Collectors.toList());

        BigDecimal avgSentiment = scored.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(scored.stream()
                        .mapToDouble(r -> r.getSentimentScore().doubleValue()).average().orElse(0))
                        .setScale(2, RoundingMode.HALF_UP);

        long positive = scored.stream().filter(r -> r.getSentimentScore().doubleValue() > 0.2).count();
        long negative = scored.stream().filter(r -> r.getSentimentScore().doubleValue() < -0.2).count();
        long neutral  = scored.size() - positive - negative;

        List<ReviewInsightDto.FlaggedReview> mostNegative = scored.stream()
                .sorted((a, b) -> a.getSentimentScore().compareTo(b.getSentimentScore()))
                .limit(3)
                .filter(r -> r.getSentimentScore().doubleValue() < -0.2)
                .map(r -> new ReviewInsightDto.FlaggedReview(
                        r.getRating(), r.getComment(), r.getSentimentScore(), r.getCreatedAt()))
                .collect(Collectors.toList());

        String mood = avgSentiment.doubleValue() > 0.2 ? "positive"
                : avgSentiment.doubleValue() < -0.2 ? "negative" : "mixed";
        String summary = String.format(
                "%d review%s, average rating %.1f/5. Overall sentiment is %s (%.2f). %d positive, %d neutral, %d negative.%s",
                reviews.size(), reviews.size() == 1 ? "" : "s", avgRating.doubleValue(), mood,
                avgSentiment.doubleValue(), positive, neutral, negative,
                negative > 0 ? " Review the flagged comments below and follow up with those customers." : "");

        return new ReviewInsightDto(reviews.size(), avgRating, avgSentiment,
                positive, neutral, negative, summary, mostNegative);
    }
}
