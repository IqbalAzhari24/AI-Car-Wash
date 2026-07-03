package com.carwash.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request body for creating or updating a catalogue service (CLERK or OWNER). */
public class ServiceRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal price;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @DecimalMin(value = "0.01")
    private BigDecimal vehicleSizeMultiplier;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getVehicleSizeMultiplier() { return vehicleSizeMultiplier; }
    public void setVehicleSizeMultiplier(BigDecimal vehicleSizeMultiplier) { this.vehicleSizeMultiplier = vehicleSizeMultiplier; }
}
