package com.carwash.backend.dto;

import com.carwash.backend.entity.Booking;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request to create a booking. {@code customerId} and {@code locationId} are optional:
 * customer defaults to the authenticated user, location to the default active branch.
 */
public class CreateBookingRequest {
    private UUID customerId;
    /** Walk-in convenience: staff may identify the customer by email instead of id. */
    private String customerEmail;
    private UUID locationId;
    private UUID serviceId;
    private LocalDateTime slotTime;
    private Booking.VehicleClass vehicleClass;
    private String vehicleModel;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID locationId) { this.locationId = locationId; }
    public UUID getServiceId() { return serviceId; }
    public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
    public LocalDateTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalDateTime slotTime) { this.slotTime = slotTime; }
    public Booking.VehicleClass getVehicleClass() { return vehicleClass; }
    public void setVehicleClass(Booking.VehicleClass vehicleClass) { this.vehicleClass = vehicleClass; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
}
