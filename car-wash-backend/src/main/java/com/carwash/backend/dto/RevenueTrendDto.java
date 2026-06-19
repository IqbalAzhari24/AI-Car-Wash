package com.carwash.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One data point in the revenue trend series.
 * Returned as an ordered list by GET /api/v1/owner/analytics/trend.
 */
public class RevenueTrendDto {

    private LocalDate date;
    private BigDecimal revenue;
    private long bookingCount;

    public RevenueTrendDto(LocalDate date, BigDecimal revenue, long bookingCount) {
        this.date         = date;
        this.revenue      = revenue;
        this.bookingCount = bookingCount;
    }

    public LocalDate   getDate()         { return date; }
    public BigDecimal  getRevenue()      { return revenue; }
    public long        getBookingCount() { return bookingCount; }
}
