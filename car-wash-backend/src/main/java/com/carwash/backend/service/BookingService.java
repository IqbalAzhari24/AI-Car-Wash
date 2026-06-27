package com.carwash.backend.service;

import com.carwash.backend.dto.BookingDto;
import com.carwash.backend.dto.CheckoutRequest;
import com.carwash.backend.dto.CheckoutResponse;
import com.carwash.backend.dto.CreateBookingRequest;
import com.carwash.backend.dto.SlotAvailabilityDto;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.Payment;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.PaymentRepository;
import com.carwash.backend.repository.ServiceRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ServiceRepository serviceRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final PaymentRepository paymentRepository;
    private final BookingEngineService bookingEngine;
    private final ToyyibPayService toyyibPayService;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          LocationRepository locationRepository,
                          ServiceRepository serviceRepository,
                          SlotCapacityRepository slotCapacityRepository,
                          PaymentRepository paymentRepository,
                          BookingEngineService bookingEngine,
                          ToyyibPayService toyyibPayService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.serviceRepository = serviceRepository;
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
        UUID serviceId = req.getServiceId();
        if (req.getSlotTime() == null || req.getVehicleClass() == null || !StringUtils.hasText(req.getVehicleModel())
                || serviceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slotTime, vehicleClass, vehicleModel and serviceId are required.");
        }

        // Customers may only book for themselves; staff may book on behalf of a customer,
        // identifying them by id or (walk-in convenience) by email.
        boolean staffOnBehalf = actingIsStaff
                && (req.getCustomerId() != null || StringUtils.hasText(req.getCustomerEmail()));
        UUID customerId;
        if (actingIsStaff && req.getCustomerId() != null) {
            customerId = req.getCustomerId();
        } else if (actingIsStaff && StringUtils.hasText(req.getCustomerEmail())) {
            customerId = userRepository.findByEmail(req.getCustomerEmail().trim())
                    .map(User::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer found with that email."));
        } else {
            customerId = actingUserId;
        }
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));

        com.carwash.backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected service is no longer available.");
        }

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
        booking.setService(service);
        booking.setSlotTime(slotTime);
        booking.setVClass(req.getVehicleClass());
        booking.setVehicleModel(req.getVehicleModel().trim());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setOverride(staffOnBehalf);
        booking.setTotalPrice(bookingEngine.calculatePrice(service, req.getVehicleClass()));

        Booking saved = bookingRepository.save(booking);
        log.info("Created booking {} for customer {} at {} ({} block(s))", saved.getId(), customerId, slotTime, requiredBlocks);
        return BookingDto.from(saved);
    }

    @Transactional(readOnly = true)
    public BookingDto getBooking(UUID bookingId, UUID actingUserId, boolean actingIsStaff) {
        return BookingDto.from(loadAuthorized(bookingId, actingUserId, actingIsStaff));
    }

    /**
     * Lists the acting customer's own bookings, newest first, so the customer
     * can track each booking's status and pay or cancel where allowed.
     *
     * @param customerId the authenticated customer's id
     * @return their bookings as DTOs, newest first
     */
    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(UUID customerId) {
        return bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(BookingDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns the active operator job queue — every booking that is CONFIRMED (ready to start)
     * or IN_PROGRESS (being washed), earliest slot first. Workers pick jobs off this list and
     * advance them via {@link #advanceStatus}.
     *
     * @return active jobs as DTOs, earliest slot first
     */
    @Transactional(readOnly = true)
    public List<BookingDto> getActiveJobs() {
        return bookingRepository.findByStatusInOrderBySlotTimeAsc(
                        List.of(Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.IN_PROGRESS))
                .stream()
                .map(BookingDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns the clerk management queue — every booking awaiting attention: PENDING
     * (needs payment confirmation), CONFIRMED (ready to wash) and IN_PROGRESS, earliest
     * slot first. Clerks confirm cash payment, advance, or cancel from this list.
     *
     * @return manageable bookings as DTOs, earliest slot first
     */
    @Transactional(readOnly = true)
    public List<BookingDto> getManageQueue() {
        return bookingRepository.findByStatusInOrderBySlotTimeAsc(
                        List.of(Booking.BookingStatus.PENDING,
                                Booking.BookingStatus.CONFIRMED,
                                Booking.BookingStatus.IN_PROGRESS))
                .stream()
                .map(BookingDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Advances a booking along the operational state machine. Only the two manual
     * forward transitions are permitted here: {@code CONFIRMED → IN_PROGRESS} (the wash
     * starts) and {@code IN_PROGRESS → COMPLETED} (the wash finishes). Any other target
     * is rejected with 409. Restricted to operators (worker/clerk/owner) at the controller.
     *
     * @param bookingId the booking to advance
     * @param target    the requested next status
     * @return the updated booking
     */
    @Transactional
    public BookingDto advanceStatus(UUID bookingId, Booking.BookingStatus target) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
        Booking.BookingStatus current = booking.getStatus();

        boolean valid =
                (current == Booking.BookingStatus.CONFIRMED && target == Booking.BookingStatus.IN_PROGRESS)
                || (current == Booking.BookingStatus.IN_PROGRESS && target == Booking.BookingStatus.COMPLETED);
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid status transition: " + current + " → " + target + ".");
        }

        booking.setStatus(target);
        bookingRepository.save(booking);
        log.info("Booking {} advanced {} -> {}", bookingId, current, target);
        return BookingDto.from(booking);
    }

    /**
     * Cancels a booking and frees the slot inventory it reserved. Only {@code PENDING}
     * or {@code CONFIRMED} bookings may be cancelled; once a wash is in progress or done
     * it cannot be undone here. The owning customer or any staff member may cancel.
     *
     * @param bookingId     the booking to cancel
     * @param actingUserId  the caller's user id
     * @param actingIsStaff whether the caller is staff (owner/clerk)
     * @return the cancelled booking
     */
    @Transactional
    public BookingDto cancelBooking(UUID bookingId, UUID actingUserId, boolean actingIsStaff) {
        Booking booking = loadAuthorized(bookingId, actingUserId, actingIsStaff);
        Booking.BookingStatus current = booking.getStatus();
        if (current != Booking.BookingStatus.PENDING && current != Booking.BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only pending or confirmed bookings can be cancelled (current: " + current + ").");
        }

        // Release every 30-minute block this booking reserved so the slot frees up.
        int blocks = bookingEngine.calculateRequiredBlocks(booking.getVClass());
        LocalDateTime blockTime = booking.getSlotTime();
        for (int i = 0; i < blocks; i++) {
            slotCapacityRepository.decrementBookedCount(booking.getLocation().getId(), blockTime);
            blockTime = blockTime.plusMinutes(30);
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} cancelled by {} (was {}), freed {} block(s)", bookingId, actingUserId, current, blocks);
        return BookingDto.from(booking);
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
     *
     * Two guards before any state change:
     *   1. HMAC checksum — rejects forged callbacks when toyyibPay is configured.
     *   2. Idempotency — silently ignores replays for already-completed payments.
     */
    @Transactional
    public void applyToyyibPayCallback(String billCode, String orderId, String status,
                                       String transactionId, String amountCents, String checksum) {
        if (toyyibPayService.isConfigured()
                && !toyyibPayService.verifyChecksum(billCode, amountCents, status, checksum)) {
            log.warn("toyyibPay callback rejected — invalid checksum for order {}", orderId);
            return;
        }

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

        // Idempotency guard — reject replays for already-completed payments.
        if ("COMPLETED".equals(payment.getPaymentStatus())) {
            log.info("toyyibPay callback ignored — booking {} already confirmed", bookingId);
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

    /**
     * Returns slot availability for the given date, sorted by time.
     * Used by the booking wizard to render the slot grid with available/full indicators.
     *
     * @param date the calendar date to query (Malaysia local time)
     * @return ordered list of {@link SlotAvailabilityDto} for each configured slot on that day
     */
    public List<SlotAvailabilityDto> listSlotsForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.plusDays(1).atStartOfDay();
        return slotCapacityRepository.findBySlotTimeBetween(start, end)
                .stream()
                .sorted(Comparator.comparing(sc -> sc.getSlotTime()))
                .map(SlotAvailabilityDto::from)
                .collect(Collectors.toList());
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
