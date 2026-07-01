package com.carwash.backend.controller;

import com.carwash.backend.dto.LocationDto;
import com.carwash.backend.repository.LocationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only branch list, used by the booking and valet flows to populate the
 * branch picker and resolve locationId.
 */
@RestController
@RequestMapping("/api/v1")
public class LocationController {

    private final LocationRepository locationRepository;

    public LocationController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @GetMapping("/locations")
    public List<LocationDto> listActive() {
        return locationRepository.findActive().stream()
                .map(LocationDto::from)
                .collect(Collectors.toList());
    }
}
