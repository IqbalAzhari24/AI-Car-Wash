package com.carwash.backend.dto;

import com.carwash.backend.entity.Booking;
import java.time.LocalDateTime;

/**
 * Request body for submitting a new valet pick-up request.
 *
 * <p>The customer's coordinates ({@code customerLat}/{@code customerLng}) are
 * captured by the browser's {@code navigator.geolocation} API on the frontend
 * and forwarded here. The backend runs the Haversine check server-side.
 */
public class CreateValetRequestDto {

    /** UUID of the target branch/location. */
    private String locationId;

    /** Customer GPS latitude (decimal degrees). */
    private Double customerLat;

    /** Customer GPS longitude (decimal degrees). */
    private Double customerLng;

    /** Optional human-readable address label (reverse-geocoded or typed). */
    private String customerAddress;

    /** Requested pick-up date and time. */
    private LocalDateTime pickupTime;

    private Booking.VehicleClass vehicleClass;
    private String vehicleModel;
    private String notes;

    // Getters and Setters
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public Double getCustomerLat() { return customerLat; }
    public void setCustomerLat(Double customerLat) { this.customerLat = customerLat; }
    public Double getCustomerLng() { return customerLng; }
    public void setCustomerLng(Double customerLng) { this.customerLng = customerLng; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }
    public Booking.VehicleClass getVehicleClass() { return vehicleClass; }
    public void setVehicleClass(Booking.VehicleClass vehicleClass) { this.vehicleClass = vehicleClass; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
