package com.carwash.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of a checkout.
 * For CASH the payment is already COMPLETED and {@code paymentUrl} is null.
 * For TOYYIBPAY the payment is PENDING and the client must redirect the user to
 * {@code paymentUrl}; the booking is confirmed later via the toyyibPay callback.
 */
public class CheckoutResponse {
    private final UUID bookingId;
    private final UUID paymentId;
    private final String method;
    private final String paymentStatus;
    private final BigDecimal amount;
    private final String paymentUrl;

    public CheckoutResponse(UUID bookingId, UUID paymentId, String method, String paymentStatus,
                            BigDecimal amount, String paymentUrl) {
        this.bookingId = bookingId;
        this.paymentId = paymentId;
        this.method = method;
        this.paymentStatus = paymentStatus;
        this.amount = amount;
        this.paymentUrl = paymentUrl;
    }

    public UUID getBookingId() { return bookingId; }
    public UUID getPaymentId() { return paymentId; }
    public String getMethod() { return method; }
    public String getPaymentStatus() { return paymentStatus; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentUrl() { return paymentUrl; }
}
