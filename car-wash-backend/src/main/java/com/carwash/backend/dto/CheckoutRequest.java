package com.carwash.backend.dto;

/**
 * Checkout instruction for a booking.
 * {@code method} is CASH (counter payment, recorded immediately) or
 * TOYYIBPAY (online — a bill is created and a payment URL is returned).
 * Payer fields are used to pre-fill the toyyibPay bill; optional for CASH.
 */
public class CheckoutRequest {
    public enum Method { CASH, TOYYIBPAY }

    private Method method;
    private String payerName;
    private String payerEmail;
    private String payerPhone;

    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getPayerEmail() { return payerEmail; }
    public void setPayerEmail(String payerEmail) { this.payerEmail = payerEmail; }
    public String getPayerPhone() { return payerPhone; }
    public void setPayerPhone(String payerPhone) { this.payerPhone = payerPhone; }
}
