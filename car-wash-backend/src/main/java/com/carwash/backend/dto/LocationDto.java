package com.carwash.backend.dto;

import com.carwash.backend.entity.Location;

import java.util.UUID;

/** Public-facing branch info — used by the booking and valet flows to pick a branch. */
public class LocationDto {
    private final UUID id;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;

    public LocationDto(UUID id, String name, String address, Double latitude, Double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static LocationDto from(Location l) {
        return new LocationDto(l.getId(), l.getName(), l.getAddress(), l.getLatitude(), l.getLongitude());
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
