package com.carwash.backend.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clerk_id")
    private User clerk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "slot_time", nullable = false)
    private LocalDateTime slotTime;

    @Column(name = "vehicle_class", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VehicleClass vClass;

    @Column(name = "vehicle_model", nullable = false, length = 100)
    private String vehicleModel;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "is_override", nullable = false)
    private Boolean isOverride = false;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal totalPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum VehicleClass {
        MOTORCYCLE, COMPACT, SEDAN, SUV_LUXURY, MPV_LARGE
    }

    public enum BookingStatus {
        PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, NO_SHOW, CANCELLED
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public User getClerk() { return clerk; }
    public void setClerk(User clerk) { this.clerk = clerk; }
    public User getWorker() { return worker; }
    public void setWorker(User worker) { this.worker = worker; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public LocalDateTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalDateTime slotTime) { this.slotTime = slotTime; }
    public VehicleClass getVClass() { return vClass; }
    public void setVClass(VehicleClass vClass) { this.vClass = vClass; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public Boolean getOverride() { return isOverride; }
    public void setOverride(Boolean override) { isOverride = override; }
    public java.math.BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(java.math.BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
