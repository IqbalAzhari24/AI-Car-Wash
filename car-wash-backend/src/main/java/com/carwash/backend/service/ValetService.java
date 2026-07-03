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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final NotificationService notificationService;

    public ValetService(ValetRequestRepository valetRepository,
                        LocationRepository locationRepository,
                        UserRepository userRepository,
                        NotificationService notificationService) {
        this.valetRepository = valetRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------------
    // Customer — submit a new valet request
    // -------------------------------------------------------------------------

    /**
     * Submits a valet pick-up request and performs the Haversine geofencing check.
     *
     * @param customerId id of the authenticated customer (JWT subject)
     * @param dto        request body containing customer GPS coordinates
     * @return the persisted valet request as a DTO (status ACCEPTED or REJECTED)
     * @throws IllegalArgumentException if the location is not found or has no coordinates set
     */
    @Transactional
    public ValetRequestDto submitRequest(String customerId, CreateValetRequestDto dto) {
        User customer = userRepository.findById(UUID.fromString(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

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
                customerId, location.getName(), distanceKm, radiusKm, withinRadius);

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
        return toDto(saved, saved.getStatus() == ValetRequest.ValetStatus.ACCEPTED);
    }

    // -------------------------------------------------------------------------
    // Customer — view own requests
    // -------------------------------------------------------------------------

    /**
     * Returns all valet requests submitted by the authenticated customer.
     *
     * @param customerId id of the authenticated customer (JWT subject)
     * @return list of valet request DTOs, newest first
     */
    @Transactional(readOnly = true)
    public List<ValetRequestDto> getMyRequests(String customerId) {
        User customer = userRepository.findById(UUID.fromString(customerId))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        return valetRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(r -> toDto(r, r.getStatus() == ValetRequest.ValetStatus.ACCEPTED))
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
                .map(r -> toDto(r, r.getStatus() == ValetRequest.ValetStatus.ACCEPTED))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Clerk / Owner — advance or cancel a request
    // -------------------------------------------------------------------------

    /**
     * Updates the status of a valet request (Clerk / Owner access).
     *
     * <p>Allowed transitions:
     * PENDING → ACCEPTED | REJECTED | CANCELLED;
     * ACCEPTED → IN_PROGRESS | CANCELLED;
     * IN_PROGRESS → COMPLETED | CANCELLED.
     * REJECTED, COMPLETED and CANCELLED are terminal.
     *
     * @param requestId valet request id
     * @param newStatus target status
     * @return the updated request as a DTO
     * @throws ResponseStatusException 404 if not found, 422 on an invalid transition
     */
    @Transactional
    public ValetRequestDto updateStatus(UUID requestId, ValetRequest.ValetStatus newStatus) {
        ValetRequest request = valetRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Valet request not found: " + requestId));

        if (!isTransitionAllowed(request.getStatus(), newStatus)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot change valet request from " + request.getStatus() + " to " + newStatus + ".");
        }

        request.setStatus(newStatus);
        ValetRequest saved = valetRepository.save(request);
        notificationService.notifyValetUpdate(saved);
        log.info("Valet request {} status changed to {}", requestId, newStatus);
        return toDto(saved, saved.getStatus() == ValetRequest.ValetStatus.ACCEPTED);
    }

    // -------------------------------------------------------------------------
    // Customer — cancel own request
    // -------------------------------------------------------------------------

    /**
     * Cancels a valet request owned by the authenticated customer.
     * Only PENDING or ACCEPTED requests can be cancelled.
     *
     * @param customerId id of the authenticated customer (JWT subject)
     * @param requestId  valet request id
     * @return the cancelled request as a DTO
     * @throws ResponseStatusException 404 if not found, 403 if owned by someone else,
     *                                 422 if the request is already in progress or finished
     */
    @Transactional
    public ValetRequestDto cancel(String customerId, UUID requestId) {
        ValetRequest request = valetRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Valet request not found: " + requestId));

        if (!request.getCustomer().getId().equals(UUID.fromString(customerId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own valet requests.");
        }
        if (request.getStatus() != ValetRequest.ValetStatus.PENDING
                && request.getStatus() != ValetRequest.ValetStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only pending or accepted valet requests can be cancelled.");
        }

        request.setStatus(ValetRequest.ValetStatus.CANCELLED);
        ValetRequest saved = valetRepository.save(request);
        return toDto(saved, false);
    }

    private boolean isTransitionAllowed(ValetRequest.ValetStatus from, ValetRequest.ValetStatus to) {
        return switch (from) {
            case PENDING -> to == ValetRequest.ValetStatus.ACCEPTED
                    || to == ValetRequest.ValetStatus.REJECTED
                    || to == ValetRequest.ValetStatus.CANCELLED;
            case ACCEPTED -> to == ValetRequest.ValetStatus.IN_PROGRESS
                    || to == ValetRequest.ValetStatus.CANCELLED;
            case IN_PROGRESS -> to == ValetRequest.ValetStatus.COMPLETED
                    || to == ValetRequest.ValetStatus.CANCELLED;
            case REJECTED, COMPLETED, CANCELLED -> false;
        };
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
