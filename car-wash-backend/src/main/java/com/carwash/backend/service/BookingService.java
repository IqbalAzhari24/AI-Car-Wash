package com.carwash.backend.service;

import com.carwash.backend.dto.BookingDto;
import com.carwash.backend.dto.CheckoutRequest;
import com.carwash.backend.dto.CheckoutResponse;
import com.carwash.backend.dto.CreateBookingRequest;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.Payment;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.PaymentRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final PaymentRepository paymentRepository;
    private final BookingEngineService bookingEngine;
    private final ToyyibPayService toyyibPayService;

    @Value("${booking.base-price:25.00}")
    private BigDecimal basePrice;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          LocationRepository locationRepository,
                          SlotCapacityRepository slotCapacityRepository,
                          PaymentRepository paymentRepository,
                          BookingEngineService bookingEngine,
                          ToyyibPayService toyyibPayService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.paymentRepository = paymentRepository;
        this.bookingEngine = bookingEngine;
        this.toyyibPayService = toyyibPayService;
    }

    /**
     * Creates a PENDING booking, atomically reserving every 30-minute block the vehicle
     * class requires. The whole method is one transaction: if any block is full (or
     * missing) the reservation throws and all prior increments roll back, so a booking
     * never leaves the slot inventory half-reserved and concurrent callers can't
     * oversubscribe a slot.
     */
    @Transactional
    public BookingDto createBooking(UUID actingUserId, boolean actingIsStaff, CreateBookingRequest req) {
        if (req.getSlotTime() == null || req.getVehicleClass() == null || !StringUtils.hasText(req.getVehicleModel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slotTime, vehicleClass and vehicleModel are required.");
        }

        // Customers may only book for themselves; staff may book on behalf of a customer.
        UUID customerId = (actingIsStaff && req.getCustomerId() != null) ? req.getCustomerId() : actingUserId;
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));

        Location location = resolveLocation(req.getLocationId());

        LocalDateTime slotTime = req.getSlotTime();
        if (slotTime.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book a slot in the past.");
        }

        int requiredBlocks = bookingEngine.calculateRequiredBlocks(req.getVehicleClass());

        // Atomically reserve each consecutive block. A 0-row update means full/missing.
        LocalDateTime blockTime = slotTime;
        for (int i = 0; i < requiredBlocks; i++) {
            int reserved = slotCapacityRepository.incrementBookedCount(location.getId(), blockTime);
            if (reserved == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Selected time is no longer available for this vehicle size.");
            }
            blockTime = blockTime.plusMinutes(30);
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setLocation(location);
        booking.setSlotTime(slotTime);
        booking.setVClass(req.getVehicleClass());
        booking.setVehicleModel(req.getVehicleModel().trim());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setOverride(actingIsStaff && req.getCustomerId() != null);
        booking.setTotalPrice(bookingEngine.calculatePrice(basePrice, req.getVehicleClass()));

        Booking saved = bookingRepository.save(booking);
        log.info("Created booking {} for customer {} at {} ({} block(s))", saved.getId(), customerId, slotTime, requiredBlocks);
        return BookingDto.from(saved);
    }

    @Transactional(readOnly = true)
    public BookingDto getBooking(UUID bookingId, UUID actingUserId, boolean actingIsStaff) {
        return BookingDto.from(loadAuthorized(bookingId, actingUserId, actingIsStaff));
    }

    /**
     * Records payment for a booking. CASH completes immediately and confirms the booking;
     * TOYYIBPAY creates a hosted bill and returns its URL, leaving the booking PENDING
     * until the gateway callback confirms it.
     */
    @Transactional
    public CheckoutResponse checkout(UUID bookingId, CheckoutRequest req, UUID actingUserId, boolean actingIsStaff) {
        if (req.getMethod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment method is required.");
        }
        Booking booking = loadAuthorized(bookingId, actingUserId, actingIsStaff);

        Payment payment = paymentRepository.findByBooking_Id(bookingId).orElseGet(() -> {
            Payment p = new Payment();
            p.setBooking(booking);
            return p;
        });

        if ("COMPLETED".equals(payment.getPaymentStatus())) {
            // Idempotent: already paid.
            return new CheckoutResponse(bookingId, payment.getId(), payment.getMethod(),
                    payment.getPaymentStatus(), payment.getAmount(), null);
        }

        BigDecimal amount = booking.getTotalPrice();
        payment.setAmount(amount);

        if (req.getMethod() == CheckoutRequest.Method.CASH) {
            payment.setMethod("CASH");
            payment.setPaymentStatus("COMPLETED");
            payment.setTransactionId(null);
            Payment savedPayment = paymentRepository.save(payment);
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Cash checkout completed for booking {}", bookingId);
            return new CheckoutResponse(bookingId, savedPayment.getId(), "CASH", "COMPLETED", amount, null);
        }

        // TOYYIBPAY
        if (!toyyibPayService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Online payment is not configured.");
        }
        payment.setMethod("TOYYIBPAY");
        payment.setPaymentStatus("PENDING");
        Payment savedPayment = paymentRepository.save(payment);

        ToyyibPayService.Bill bill = toyyibPayService.createBill(
                "Timah Wash Booking",
                "Car wash booking " + shortId(bookingId),
                amount,
                bookingId.toString(),
                req.getPayerName(),
                req.getPayerEmail(),
                req.getPayerPhone());

        // Hold the bill code until the callback supplies the real transaction id.
        savedPayment.setTransactionId(bill.billCode);
        paymentRepository.save(savedPayment);
        log.info("toyyibPay bill {} created for booking {}", bill.billCode, bookingId);
        return new CheckoutResponse(bookingId, savedPayment.getId(), "TOYYIBPAY", "PENDING", amount, bill.paymentUrl);
    }

    /**
     * Applies a toyyibPay callback. {@code orderId} is the booking id we passed as the
     * external reference; {@code status} is 1=success, 2=pending, 3=fail.
     */
    @Transactional
    public void applyToyyibPayCallback(String orderId, String status, String transactionId) {
        UUID bookingId;
        try {
            bookingId = UUID.fromString(orderId);
        } catch (Exception e) {
            log.warn("toyyibPay callback with unparseable order id: {}", orderId);
            return;
        }
        Payment payment = paymentRepository.findByBooking_Id(bookingId).orElse(null);
        if (payment == null) {
            log.warn("toyyibPay callback for unknown booking {}", bookingId);
            return;
        }
        if ("1".equals(status)) {
            payment.setPaymentStatus("COMPLETED");
            if (StringUtils.hasText(transactionId)) {
                payment.setTransactionId(transactionId);
            }
            paymentRepository.save(payment);
            bookingRepository.findById(bookingId).ifPresent(b -> {
                b.setStatus(Booking.BookingStatus.CONFIRMED);
                bookingRepository.save(b);
            });
            log.info("toyyibPay payment confirmed for booking {}", bookingId);
        } else if ("3".equals(status)) {
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
            log.info("toyyibPay payment failed for booking {}", bookingId);
        }
        // status 2 (pending): leave as-is.
    }

    private Booking loadAuthorized(UUID bookingId, UUID actingUserId, boolean actingIsStaff) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
        boolean owner = booking.getCustomer() != null && booking.getCustomer().getId().equals(actingUserId);
        if (!actingIsStaff && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this booking.");
        }
        return booking;
    }

    private Location resolveLocation(UUID requestedLocationId) {
        if (requestedLocationId != null) {
            return locationRepository.findById(requestedLocationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found."));
        }
        List<Location> active = locationRepository.findActive();
        if (active.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No active location configured.");
        }
        return active.get(0);
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
