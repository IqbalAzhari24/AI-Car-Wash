package com.carwash.backend.dto;

import com.carwash.backend.entity.Booking;

/** Body for PATCH /bookings/{id}/status — the target booking status to advance to. */
public class UpdateBookingStatusRequest {
    private Booking.BookingStatus status;

    public Booking.BookingStatus getStatus() { return status; }
    public void setStatus(Booking.BookingStatus status) { this.status = status; }
}
