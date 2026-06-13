package com.carwash.backend.dto;

import com.carwash.backend.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingDto {
    private final UUID id;
    private final UUID customerId;
    private final UUID locationId;
    private final LocalDateTime slotTime;
    private final String vehicleClass;
    private final String vehicleModel;
    private final String status;
    private final BigDecimal totalPrice;
    private final LocalDateTime createdAt;

    public BookingDto(UUID id, UUID customerId, UUID locationId, LocalDateTime slotTime, String vehicleClass,
                      String vehicleModel, String status, BigDecimal totalPrice, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.locationId = locationId;
        this.slotTime = slotTime;
        this.vehicleClass = vehicleClass;
        this.vehicleModel = vehicleModel;
        this.status = status;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
    }

    public static BookingDto from(Booking b) {
        return new BookingDto(
                b.getId(),
                b.getCustomer() != null ? b.getCustomer().getId() : null,
                b.getLocation() != null ? b.getLocation().getId() : null,
                b.getSlotTime(),
                b.getVClass() != null ? b.getVClass().name() : null,
                b.getVehicleModel(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getTotalPrice(),
                b.getCreatedAt());
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getLocationId() { return locationId; }
    public LocalDateTime getSlotTime() { return slotTime; }
    public String getVehicleClass() { return vehicleClass; }
    public String getVehicleModel() { return vehicleModel; }
    public String getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
