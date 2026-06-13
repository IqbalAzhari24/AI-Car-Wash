package com.carwash.backend.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "services")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "vehicle_size_multiplier", precision = 3, scale = 2)
    private BigDecimal vehicleSizeMultiplier = BigDecimal.ONE;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
}
