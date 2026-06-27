package com.carwash.backend.dto;

import com.carwash.backend.entity.ValetRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a valet request.
 *
 * <p>Entity fields are mapped here — JPA entities are never serialised
 * directly to JSON per CLAUDE.md convention.
 */
public class ValetRequestDto {

    private UUID id;
    private UUID customerId;
    private String customerEmail;
    private UUID locationId;
    private String locationName;
    private Double customerLat;
    private Double customerLng;
    private String customerAddress;
    private Double distanceKm;
    private Double radiusKm;
    private boolean withinRadius;
    private LocalDateTime pickupTime;
    private String vehicleClass;
    private String vehicleModel;
    private ValetRequest.ValetStatus status;
    private String notes;
    private LocalDateTime createdAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID locationId) { this.locationId = locationId; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public Double getCustomerLat() { return customerLat; }
    public void setCustomerLat(Double customerLat) { this.customerLat = customerLat; }
    public Double getCustomerLng() { return customerLng; }
    public void setCustomerLng(Double customerLng) { this.customerLng = customerLng; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(Double radiusKm) { this.radiusKm = radiusKm; }
    public boolean isWithinRadius() { return withinRadius; }
    public void setWithinRadius(boolean withinRadius) { this.withinRadius = withinRadius; }
    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }
    public String getVehicleClass() { return vehicleClass; }
    public void setVehicleClass(String vehicleClass) { this.vehicleClass = vehicleClass; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public ValetRequest.ValetStatus getStatus() { return status; }
    public void setStatus(ValetRequest.ValetStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
