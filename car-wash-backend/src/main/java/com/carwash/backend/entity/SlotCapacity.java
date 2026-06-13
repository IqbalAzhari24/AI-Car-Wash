package com.carwash.backend.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "slot_capacities")
public class SlotCapacity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "slot_time", nullable = false)
    private LocalDateTime slotTime;

    @Column(name = "max_limit", nullable = false)
    private Integer maxLimit;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public LocalDateTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalDateTime slotTime) { this.slotTime = slotTime; }
    public Integer getMaxLimit() { return maxLimit; }
    public void setMaxLimit(Integer maxLimit) { this.maxLimit = maxLimit; }
    public Integer getBookedCount() { return bookedCount; }
    public void setBookedCount(Integer bookedCount) { this.bookedCount = bookedCount; }
}
