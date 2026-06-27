package com.carwash.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Valet request — a separate domain object from {@link Booking}.
 *
 * <p>A valet request represents a pick-up job where the worker travels to
 * the customer's location. The customer supplies GPS coordinates via the
 * browser's {@code navigator.geolocation} API; the backend validates the
 * distance against the shop's configurable Haversine radius.
 *
 * <p>Per CLAUDE.md: Valet and Booking entities MUST remain separate.
 */
@Entity
@Table(name = "valet_requests")
public class ValetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /** Customer's GPS latitude from browser geolocation. */
    @Column(name = "customer_lat", nullable = false)
    private Double customerLat;

    /** Customer's GPS longitude from browser geolocation. */
    @Column(name = "customer_lng", nullable = false)
    private Double customerLng;

    /** Optional human-readable address label provided by the customer. */
    @Column(name = "customer_address", length = 255)
    private String customerAddress;

    /** Haversine distance (km) computed at request time — stored for audit. */
    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    /** The branch's configured service radius (km) at time of request — stored for audit. */
    @Column(name = "radius_km", nullable = false)
    private Double radiusKm;

    @Column(name = "pickup_time", nullable = false)
    private LocalDateTime pickupTime;

    @Column(name = "vehicle_class", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Booking.VehicleClass vehicleClass;

    @Column(name = "vehicle_model", nullable = false, length = 100)
    private String vehicleModel;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ValetStatus status = ValetStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ValetStatus {
        PENDING, ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
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
    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }
    public Booking.VehicleClass getVehicleClass() { return vehicleClass; }
    public void setVehicleClass(Booking.VehicleClass vehicleClass) { this.vehicleClass = vehicleClass; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public ValetStatus getStatus() { return status; }
    public void setStatus(ValetStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
