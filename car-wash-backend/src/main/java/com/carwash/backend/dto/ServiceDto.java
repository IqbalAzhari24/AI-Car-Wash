package com.carwash.backend.dto;

import com.carwash.backend.entity.Service;

import java.math.BigDecimal;
import java.util.UUID;

public class ServiceDto {
    private final UUID id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer durationMinutes;
    private final BigDecimal vehicleSizeMultiplier;

    public ServiceDto(UUID id, String name, String description, BigDecimal price,
                      Integer durationMinutes, BigDecimal vehicleSizeMultiplier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.vehicleSizeMultiplier = vehicleSizeMultiplier;
    }

    public static ServiceDto from(Service s) {
        return new ServiceDto(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getPrice(),
                s.getDurationMinutes(),
                s.getVehicleSizeMultiplier());
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public BigDecimal getVehicleSizeMultiplier() { return vehicleSizeMultiplier; }
}
