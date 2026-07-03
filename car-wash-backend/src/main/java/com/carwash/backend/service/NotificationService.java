package com.carwash.backend.service;

import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.ValetRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Real-time customer notifications, pushed over the existing STOMP broker to
 * each customer's private queue {@code /user/queue/updates} (same mechanism as
 * Timah's {@code /user/queue/timah-reply}).
 *
 * <p>Delivery is best-effort: a customer who isn't connected simply misses the
 * push and sees the new status on their next page load. Publishing never
 * throws — a broker hiccup must not roll back the booking transaction.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Pushes a booking status update to the booking's customer.
     *
     * @param booking the booking whose status just changed (inside the caller's transaction)
     */
    public void notifyBookingUpdate(Booking booking) {
        String message = switch (booking.getStatus()) {
            case CONFIRMED   -> "Payment received — your booking is confirmed.";
            case IN_PROGRESS -> Boolean.TRUE.equals(booking.getPickupRequested())
                    ? "Your wash has started. Our driver handles pick-up as scheduled."
                    : "Your wash has started.";
            case COMPLETED   -> Boolean.TRUE.equals(booking.getDeliveryRequested())
                    ? "Your wash is complete — your car is on its way back to you."
                    : "Your wash is complete. See you at the counter!";
            case CANCELLED   -> "Your booking has been cancelled.";
            case NO_SHOW     -> "Your booking was marked as a no-show.";
            case PENDING     -> "Your booking is awaiting payment.";
        };
        send(booking.getCustomer().getId(), Map.of(
                "kind", "BOOKING",
                "id", booking.getId().toString(),
                "status", booking.getStatus().name(),
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Pushes a valet request status update to the request's customer.
     *
     * @param request the valet request whose status just changed
     */
    public void notifyValetUpdate(ValetRequest request) {
        String message = switch (request.getStatus()) {
            case ACCEPTED    -> "Your valet request was accepted.";
            case REJECTED    -> "Your valet request was rejected — outside the service area.";
            case IN_PROGRESS -> "Our driver is on the way to pick up your car.";
            case COMPLETED   -> "Valet job complete — your car is back with you.";
            case CANCELLED   -> "Your valet request has been cancelled.";
            case PENDING     -> "Your valet request is pending review.";
        };
        send(request.getCustomer().getId(), Map.of(
                "kind", "VALET",
                "id", request.getId().toString(),
                "status", request.getStatus().name(),
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    private void send(UUID customerId, Map<String, String> payload) {
        try {
            messagingTemplate.convertAndSendToUser(customerId.toString(), "/queue/updates", payload);
        } catch (Exception e) {
            // ponytail: best-effort push — never fail the business transaction over a broker error
            log.warn("Could not push notification to customer {}: {}", customerId, e.getMessage());
        }
    }
}
