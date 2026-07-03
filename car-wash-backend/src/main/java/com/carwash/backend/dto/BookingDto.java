package com.carwash.backend.dto;

import com.carwash.backend.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingDto {
    private final UUID id;
    private final UUID customerId;
    private final UUID locationId;
    private final UUID serviceId;
    private final String serviceName;
    private final LocalDateTime slotTime;
    private final String vehicleClass;
    private final String vehicleModel;
    private final String status;
    private final BigDecimal totalPrice;
    private final boolean pickupRequested;
    private final boolean deliveryRequested;
    private final String pickupAddress;
    private final String pickupNotes;
    private final LocalDateTime createdAt;

    public BookingDto(UUID id, UUID customerId, UUID locationId, UUID serviceId, String serviceName,
                      LocalDateTime slotTime, String vehicleClass, String vehicleModel, String status,
                      BigDecimal totalPrice, boolean pickupRequested, boolean deliveryRequested,
                      String pickupAddress, String pickupNotes, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.locationId = locationId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.slotTime = slotTime;
        this.vehicleClass = vehicleClass;
        this.vehicleModel = vehicleModel;
        this.status = status;
        this.totalPrice = totalPrice;
        this.pickupRequested = pickupRequested;
        this.deliveryRequested = deliveryRequested;
        this.pickupAddress = pickupAddress;
        this.pickupNotes = pickupNotes;
        this.createdAt = createdAt;
    }

    public static BookingDto from(Booking b) {
        return new BookingDto(
                b.getId(),
                b.getCustomer() != null ? b.getCustomer().getId() : null,
                b.getLocation() != null ? b.getLocation().getId() : null,
                b.getService() != null ? b.getService().getId() : null,
                b.getService() != null ? b.getService().getName() : null,
                b.getSlotTime(),
                b.getVClass() != null ? b.getVClass().name() : null,
                b.getVehicleModel(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getTotalPrice(),
                Boolean.TRUE.equals(b.getPickupRequested()),
                Boolean.TRUE.equals(b.getDeliveryRequested()),
                b.getPickupAddress(),
                b.getPickupNotes(),
                b.getCreatedAt());
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getLocationId() { return locationId; }
    public UUID getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public LocalDateTime getSlotTime() { return slotTime; }
    public String getVehicleClass() { return vehicleClass; }
    public String getVehicleModel() { return vehicleModel; }
    public String getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public boolean isPickupRequested() { return pickupRequested; }
    public boolean isDeliveryRequested() { return deliveryRequested; }
    public String getPickupAddress() { return pickupAddress; }
    public String getPickupNotes() { return pickupNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
