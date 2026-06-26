package com.carwash.backend.service;

import com.carwash.backend.dto.CreateValetRequestDto;
import com.carwash.backend.dto.ValetRequestDto;
import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.User;
import com.carwash.backend.entity.ValetRequest;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.UserRepository;
import com.carwash.backend.repository.ValetRequestRepository;
import com.carwash.backend.util.HaversineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Valet service — handles geofencing validation and request lifecycle.
 *
 * <p>The geofencing flow:
 * <ol>
 *   <li>Customer submits GPS coordinates (from browser {@code navigator.geolocation}).</li>
 *   <li>Backend loads the target branch's stored lat/lng and configured radius.</li>
 *   <li>{@link HaversineUtil} computes the great-circle distance.</li>
 *   <li>If within radius → status {@code ACCEPTED}; otherwise → {@code REJECTED}.</li>
 *   <li>Distance and radius are persisted for audit / analytics.</li>
 * </ol>
 *
 * <p>Default service radius is {@value #DEFAULT_RADIUS_KM} km; this is overridden
 * by a per-branch setting once that owner-settings feature is implemented.
 */
@Service
public class ValetService {

    private static final Logger log = LoggerFactory.getLogger(ValetService.class);

    /**
     * Fallback radius used when the branch does not yet have a configured radius.
     * Replace with a DB-backed owner setting when that feature is built.
     */
    private static final double DEFAULT_RADIUS_KM = 10.0;

    private final ValetRequestRepository valetRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public ValetService(ValetRequestRepository valetRepository,
                        LocationRepository locationRepository,
                        UserRepository userRepository) {
        this.valetRepository = valetRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Customer — submit a new valet request
    // -------------------------------------------------------------------------

    /**
     * Submits a valet pick-up request and performs the Haversine geofencing check.
     *
     * @param customerEmail email of the authenticated customer
     * @param dto           request body containing customer GPS coordinates
     * @return the persisted valet request as a DTO (status ACCEPTED or REJECTED)
     * @throws IllegalArgumentException if the location is not found or has no coordinates set
     */
    @Transactional
    public ValetRequestDto submitRequest(String customerEmail, CreateValetRequestDto dto) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerEmail));

        Location location = locationRepository.findById(UUID.fromString(dto.getLocationId()))
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + dto.getLocationId()));

        if (location.getLatitude() == null || location.getLongitude() == null) {
            throw new IllegalStateException(
                    "Branch '" + location.getName() + "' does not have GPS coordinates configured. "
                    + "Please ask the owner to set the branch location.");
        }

        // --- Haversine geofencing check ---
        double distanceKm = HaversineUtil.distanceKm(
                dto.getCustomerLat(), dto.getCustomerLng(),
                location.getLatitude(), location.getLongitude()
        );
        double radiusKm = DEFAULT_RADIUS_KM; // TODO: replace with owner-configured radius
        boolean withinRadius = distanceKm <= radiusKm;

        log.info("Valet request from customer={} to branch={}: distance={:.2f}km, radius={:.2f}km, withinRadius={}",
                customerEmail, location.getName(), distanceKm, radiusKm, withinRadius);

        // --- Persist ---
        ValetRequest request = new ValetRequest();
        request.setCustomer(customer);
        request.setLocation(location);
        request.setCustomerLat(dto.getCustomerLat());
        request.setCustomerLng(dto.getCustomerLng());
        request.setCustomerAddress(dto.getCustomerAddress());
        request.setDistanceKm(distanceKm);
        request.setRadiusKm(radiusKm);
        request.setPickupTime(dto.getPickupTime());
        request.setVehicleClass(dto.getVehicleClass());
        request.setVehicleModel(dto.getVehicleModel());
        request.setNotes(dto.getNotes());
        request.setStatus(withinRadius ? ValetRequest.ValetStatus.ACCEPTED
                                       : ValetRequest.ValetStatus.REJECTED);

        ValetRequest saved = valetRepository.save(request);
        return toDto(saved, withinRadius);
    }

    // -------------------------------------------------------------------------
    // Customer — view own requests
    // -------------------------------------------------------------------------

    /**
     * Returns all valet requests submitted by the authenticated customer.
     *
     * @param customerEmail email of the authenticated customer
     * @return list of valet request DTOs, newest first
     */
    @Transactional(readOnly = true)
    public List<ValetRequestDto> getMyRequests(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerEmail));

        return valetRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(r -> toDto(r, r.getDistanceKm() <= r.getRadiusKm()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Clerk / Owner — view requests for a branch
    // -------------------------------------------------------------------------

    /**
     * Returns all valet requests for the given branch (Clerk / Owner access).
     *
     * @param locationId UUID of the branch
     * @return list of valet request DTOs, newest first
     */
    @Transactional(readOnly = true)
    public List<ValetRequestDto> getRequestsForLocation(UUID locationId) {
        return valetRepository.findByLocation_IdOrderByCreatedAtDesc(locationId)
                .stream()
                .map(r -> toDto(r, r.getDistanceKm() <= r.getRadiusKm()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------------

    private ValetRequestDto toDto(ValetRequest r, boolean withinRadius) {
        ValetRequestDto dto = new ValetRequestDto();
        dto.setId(r.getId());
        dto.setCustomerId(r.getCustomer().getId());
        dto.setCustomerEmail(r.getCustomer().getEmail());
        dto.setLocationId(r.getLocation().getId());
        dto.setLocationName(r.getLocation().getName());
        dto.setCustomerLat(r.getCustomerLat());
        dto.setCustomerLng(r.getCustomerLng());
        dto.setCustomerAddress(r.getCustomerAddress());
        dto.setDistanceKm(r.getDistanceKm());
        dto.setRadiusKm(r.getRadiusKm());
        dto.setWithinRadius(withinRadius);
        dto.setPickupTime(r.getPickupTime());
        dto.setVehicleClass(r.getVehicleClass() != null ? r.getVehicleClass().name() : null);
        dto.setVehicleModel(r.getVehicleModel());
        dto.setStatus(r.getStatus());
        dto.setNotes(r.getNotes());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
