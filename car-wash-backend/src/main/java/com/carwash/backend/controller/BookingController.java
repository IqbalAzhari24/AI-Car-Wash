package com.carwash.backend.controller;

import com.carwash.backend.dto.BookingDto;
import com.carwash.backend.dto.CheckoutRequest;
import com.carwash.backend.dto.CheckoutResponse;
import com.carwash.backend.dto.CreateBookingRequest;
import com.carwash.backend.dto.SlotAvailabilityDto;
import com.carwash.backend.dto.UpdateBookingStatusRequest;
import com.carwash.backend.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingDto> create(@RequestBody CreateBookingRequest req, Authentication auth) {
        BookingDto dto = bookingService.createBooking(actingUserId(auth), isStaff(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/bookings/{id}")
    public BookingDto get(@PathVariable UUID id, Authentication auth) {
        return bookingService.getBooking(id, actingUserId(auth), isStaff(auth));
    }

    /** Returns the authenticated customer's own bookings, newest first. */
    @GetMapping("/bookings/mine")
    public List<BookingDto> mine(Authentication auth) {
        return bookingService.getMyBookings(actingUserId(auth));
    }

    /** Active job queue (CONFIRMED + IN_PROGRESS) for operators to work through. */
    @GetMapping("/bookings/jobs")
    @PreAuthorize("hasAnyRole('OWNER', 'CLERK', 'WORKER')")
    public List<BookingDto> jobs() {
        return bookingService.getActiveJobs();
    }

    /** Clerk management queue (PENDING + CONFIRMED + IN_PROGRESS). Clerk/Owner only. */
    @GetMapping("/bookings/manage")
    @PreAuthorize("hasAnyRole('OWNER', 'CLERK')")
    public List<BookingDto> manage() {
        return bookingService.getManageQueue();
    }

    /**
     * Advances a booking's status (CONFIRMED→IN_PROGRESS→COMPLETED). Operators only.
     */
    @PatchMapping("/bookings/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'CLERK', 'WORKER')")
    public BookingDto updateStatus(@PathVariable UUID id, @RequestBody UpdateBookingStatusRequest req) {
        if (req.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target status is required.");
        }
        return bookingService.advanceStatus(id, req.getStatus());
    }

    /** Cancels a booking (PENDING/CONFIRMED only). Owning customer or staff. */
    @PostMapping("/bookings/{id}/cancel")
    public BookingDto cancel(@PathVariable UUID id, Authentication auth) {
        return bookingService.cancelBooking(id, actingUserId(auth), isStaff(auth));
    }

    @PostMapping("/bookings/{id}/checkout")
    public CheckoutResponse checkout(@PathVariable UUID id, @RequestBody CheckoutRequest req, Authentication auth) {
        return bookingService.checkout(id, req, actingUserId(auth), isStaff(auth));
    }

    /**
     * Returns slot availability for the given date so the booking wizard can render the slot grid.
     * Requires a valid JWT — accessible to all authenticated roles.
     */
    @GetMapping("/slots")
    public List<SlotAvailabilityDto> listSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.listSlotsForDate(date);
    }

    /**
     * Server-to-server callback from toyyibPay (no JWT — permitted in WebSecurityConfig).
     * order_id is the booking id we set as the external reference; status is 1=success, 2=pending, 3=fail.
     * checksum is MD5(billcode + categoryCode + billPaymentAmount + billPaymentStatus + userSecretKey).
     */
    @PostMapping("/payments/toyyibpay/callback")
    public ResponseEntity<String> toyyibPayCallback(
            @RequestParam(name = "billcode", required = false) String billCode,
            @RequestParam(name = "order_id", required = false) String orderId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "transaction_id", required = false) String transactionId,
            @RequestParam(name = "refno", required = false) String refno,
            @RequestParam(name = "amount", required = false) String amount,
            @RequestParam(name = "checksum", required = false) String checksum) {
        String txn = transactionId != null ? transactionId : refno;
        bookingService.applyToyyibPayCallback(billCode, orderId, status, txn, amount, checksum);
        return ResponseEntity.ok("OK");
    }

    // --- helpers: the JWT filter sets principal name = user id and authority ROLE_<role> ---

    private UUID actingUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    private boolean isStaff(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            String role = a.getAuthority();
            if ("ROLE_OWNER".equals(role) || "ROLE_CLERK".equals(role)) {
                return true;
            }
        }
        return false;
    }
}
