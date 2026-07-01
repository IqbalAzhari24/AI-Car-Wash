package com.carwash.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Thin client for the toyyibPay payment gateway (https://toyyibpay.com).
 * Creates a "bill" and returns the hosted payment URL the customer is redirected to.
 * All credentials/URLs are externalised so they can be swapped between the sandbox
 * (dev.toyyibpay.com) and production without code changes.
 */
@Service
public class ToyyibPayService {

    private static final Logger log = LoggerFactory.getLogger(ToyyibPayService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${toyyibpay.base-url:https://dev.toyyibpay.com}")
    private String baseUrl;
    @Value("${toyyibpay.secret-key:}")
    private String secretKey;
    @Value("${toyyibpay.category-code:}")
    private String categoryCode;
    @Value("${toyyibpay.return-url:http://localhost:5173/checkout/return}")
    private String returnUrl;
    @Value("${toyyibpay.callback-url:http://localhost:8080/api/v1/payments/toyyibpay/callback}")
    private String callbackUrl;

    public ToyyibPayService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(secretKey) && StringUtils.hasText(categoryCode);
    }

    /** Holds the bill code and the URL the customer should be redirected to. */
    public static final class Bill {
        public final String billCode;
        public final String paymentUrl;
        public Bill(String billCode, String paymentUrl) {
            this.billCode = billCode;
            this.paymentUrl = paymentUrl;
        }
    }

    /**
     * Creates a fixed-amount bill for a booking and returns the hosted payment URL.
     * @param amount ringgit amount (converted to cents for the API)
     * @param externalRef our booking id, echoed back to us in the callback as order_id
     */
    public Bill createBill(String billName, String billDescription, BigDecimal amount, String externalRef,
                           String payerName, String payerEmail, String payerPhone) {
        if (!isConfigured()) {
            throw new IllegalStateException("toyyibPay is not configured (set toyyibpay.secret-key and toyyibpay.category-code).");
        }
        long amountCents = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("userSecretKey", secretKey);
        form.add("categoryCode", categoryCode);
        form.add("billName", sanitizeName(billName, 30));
        form.add("billDescription", sanitizeName(billDescription, 100));
        form.add("billPriceSetting", "1");          // fixed price
        form.add("billPayorInfo", "1");             // collect payer info
        form.add("billAmount", String.valueOf(amountCents));
        form.add("billReturnUrl", returnUrl);
        form.add("billCallbackUrl", callbackUrl);
        form.add("billExternalReferenceNo", externalRef);
        form.add("billTo", StringUtils.hasText(payerName) ? payerName : "Customer");
        form.add("billEmail", StringUtils.hasText(payerEmail) ? payerEmail : "noreply@timahwash.com");
        form.add("billPhone", StringUtils.hasText(payerPhone) ? payerPhone : "0000000000");
        form.add("billPaymentChannel", "2");        // 0=FPX, 1=card, 2=both

        String response = webClient.post()
                .uri(baseUrl + "/index.php/api/createBill")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.isArray() && root.size() > 0 && root.get(0).hasNonNull("BillCode")) {
                String billCode = root.get(0).get("BillCode").asText();
                String paymentUrl = baseUrl + "/" + billCode;
                log.info("Created toyyibPay bill {} for booking ref {}", billCode, externalRef);
                return new Bill(billCode, paymentUrl);
            }
        } catch (Exception e) {
            log.error("Failed to parse toyyibPay createBill response: {}", response, e);
            throw new IllegalStateException("Unexpected toyyibPay response.", e);
        }
        throw new IllegalStateException("toyyibPay rejected the bill: " + response);
    }

    /**
     * Verifies the HMAC checksum included in toyyibPay server-to-server callbacks.
     * Formula: MD5(billCode + categoryCode + billPaymentAmount + billPaymentStatus + userSecretKey)
     *
     * @param billCode    the bill code echoed back by toyyibPay
     * @param amountCents payment amount in cents as a string (e.g. "2500" for RM 25.00)
     * @param status      payment status from the callback (1/2/3)
     * @param checksum    the checksum value from the callback request
     */
    public boolean verifyChecksum(String billCode, String amountCents, String status, String checksum) {
        if (!StringUtils.hasText(checksum) || !StringUtils.hasText(billCode)) {
            return false;
        }
        String raw = billCode + categoryCode + amountCents + status + secretKey;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String hex = HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
            return checksum.equalsIgnoreCase(hex);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    /** toyyibPay restricts bill name/description to alphanumeric, space and underscore. */
    private String sanitizeName(String value, int max) {
        String cleaned = (value == null ? "" : value).replaceAll("[^A-Za-z0-9 _]", " ").trim();
        if (cleaned.isEmpty()) cleaned = "Timah Wash";
        return cleaned.length() > max ? cleaned.substring(0, max) : cleaned;
    }
}
