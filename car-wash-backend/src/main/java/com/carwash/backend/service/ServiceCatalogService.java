package com.carwash.backend.service;

import com.carwash.backend.dto.ServiceDto;
import com.carwash.backend.dto.ServiceRequest;
import com.carwash.backend.repository.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Clerk/Owner-managed CRUD for the wash-service catalogue.
 *
 * <p>Deletion is a soft deactivate ({@code is_active = false}) so historical
 * bookings keep their service reference.
 */
@Service
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * Returns all services including deactivated ones — for the clerk/owner console.
     *
     * @return every catalogue service, cheapest first
     */
    @Transactional(readOnly = true)
    public List<ServiceDto> listAll() {
        return serviceRepository.findAll().stream()
                .sorted((a, b) -> a.getPrice().compareTo(b.getPrice()))
                .map(ServiceDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new catalogue service.
     *
     * @param request validated service fields
     * @return the persisted service as a DTO
     */
    @Transactional
    public ServiceDto create(ServiceRequest request) {
        com.carwash.backend.entity.Service s = new com.carwash.backend.entity.Service();
        apply(s, request);
        return ServiceDto.from(serviceRepository.save(s));
    }

    /**
     * Full update of an existing catalogue service.
     *
     * @param id      service id
     * @param request validated replacement fields
     * @return the updated service as a DTO
     * @throws ResponseStatusException 404 if the service does not exist
     */
    @Transactional
    public ServiceDto update(UUID id, ServiceRequest request) {
        com.carwash.backend.entity.Service s = find(id);
        apply(s, request);
        return ServiceDto.from(serviceRepository.save(s));
    }

    /**
     * Soft-deletes (deactivates) a service so it no longer appears in the
     * booking flow. Historical bookings keep their reference.
     *
     * @param id service id
     * @throws ResponseStatusException 404 if the service does not exist
     */
    @Transactional
    public void deactivate(UUID id) {
        com.carwash.backend.entity.Service s = find(id);
        s.setActive(false);
        serviceRepository.save(s);
    }

    private com.carwash.backend.entity.Service find(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + id));
    }

    private void apply(com.carwash.backend.entity.Service s, ServiceRequest r) {
        s.setName(r.getName().trim());
        s.setDescription(r.getDescription());
        s.setPrice(r.getPrice());
        s.setDurationMinutes(r.getDurationMinutes());
        s.setVehicleSizeMultiplier(r.getVehicleSizeMultiplier() != null ? r.getVehicleSizeMultiplier() : BigDecimal.ONE);
        if (s.getActive() == null) s.setActive(true);
    }
}
