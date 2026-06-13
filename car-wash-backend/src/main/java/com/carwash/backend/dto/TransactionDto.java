package com.carwash.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single line in a customer's transaction history: booking details plus the
 * associated payment (if one exists). Amount comes from the Payment record since
 * the Booking entity does not map total_price.
 */
public class TransactionDto {
    private final UUID bookingId;
    private final LocalDateTime slotTime;
    private final String vehicleClass;
    private final String bookingStatus;
    private final LocalDateTime createdAt;
    private final BigDecimal amount;
    private final String paymentStatus;
    private final String transactionId;

    public TransactionDto(UUID bookingId, LocalDateTime slotTime, String vehicleClass, String bookingStatus,
                          LocalDateTime createdAt, BigDecimal amount, String paymentStatus, String transactionId) {
        this.bookingId = bookingId;
        this.slotTime = slotTime;
        this.vehicleClass = vehicleClass;
        this.bookingStatus = bookingStatus;
        this.createdAt = createdAt;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
    }

    public UUID getBookingId() { return bookingId; }
    public LocalDateTime getSlotTime() { return slotTime; }
    public String getVehicleClass() { return vehicleClass; }
    public String getBookingStatus() { return bookingStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getTransactionId() { return transactionId; }
}
