package com.carwash.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily sales summary returned by GET /api/v1/owner/analytics/summary.
 * All monetary values are in MYR. Revenue counts only COMPLETED payments.
 */
public class SalesSummaryDto {

    private LocalDate date;

    // Revenue
    private BigDecimal totalRevenue;
    private BigDecimal cashRevenue;
    private BigDecimal onlineRevenue;

    // Payment counts
    private long completedPayments;
    private long pendingPayments;
    private long failedPayments;

    // Booking counts
    private long totalBookings;
    private long completedBookings;
    private long confirmedBookings;
    private long pendingBookings;
    private long cancelledBookings;
    private long noShowBookings;

    // Getters and setters

    public LocalDate getDate()                    { return date; }
    public void setDate(LocalDate date)           { this.date = date; }

    public BigDecimal getTotalRevenue()           { return totalRevenue; }
    public void setTotalRevenue(BigDecimal v)     { this.totalRevenue = v; }

    public BigDecimal getCashRevenue()            { return cashRevenue; }
    public void setCashRevenue(BigDecimal v)      { this.cashRevenue = v; }

    public BigDecimal getOnlineRevenue()          { return onlineRevenue; }
    public void setOnlineRevenue(BigDecimal v)    { this.onlineRevenue = v; }

    public long getCompletedPayments()            { return completedPayments; }
    public void setCompletedPayments(long v)      { this.completedPayments = v; }

    public long getPendingPayments()              { return pendingPayments; }
    public void setPendingPayments(long v)        { this.pendingPayments = v; }

    public long getFailedPayments()               { return failedPayments; }
    public void setFailedPayments(long v)         { this.failedPayments = v; }

    public long getTotalBookings()                { return totalBookings; }
    public void setTotalBookings(long v)          { this.totalBookings = v; }

    public long getCompletedBookings()            { return completedBookings; }
    public void setCompletedBookings(long v)      { this.completedBookings = v; }

    public long getConfirmedBookings()            { return confirmedBookings; }
    public void setConfirmedBookings(long v)      { this.confirmedBookings = v; }

    public long getPendingBookings()              { return pendingBookings; }
    public void setPendingBookings(long v)        { this.pendingBookings = v; }

    public long getCancelledBookings()            { return cancelledBookings; }
    public void setCancelledBookings(long v)      { this.cancelledBookings = v; }

    public long getNoShowBookings()               { return noShowBookings; }
    public void setNoShowBookings(long v)         { this.noShowBookings = v; }
}
