package com.carwash.backend.controller;

import com.carwash.backend.dto.ServiceDto;
import com.carwash.backend.dto.ServiceRequest;
import com.carwash.backend.repository.ServiceRepository;
import com.carwash.backend.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service catalogue endpoints.
 *
 * <pre>
 * GET    /api/v1/services          — any authenticated user: active services (booking flow)
 * GET    /api/v1/services/all      — CLERK|OWNER: all services incl. deactivated
 * POST   /api/v1/services          — CLERK|OWNER: create
 * PUT    /api/v1/services/{id}     — CLERK|OWNER: full update
 * DELETE /api/v1/services/{id}     — CLERK|OWNER: soft-delete (deactivate)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final ServiceCatalogService serviceCatalogService;

    public ServiceController(ServiceRepository serviceRepository,
                             ServiceCatalogService serviceCatalogService) {
        this.serviceRepository = serviceRepository;
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping("/services")
    public List<ServiceDto> listActive() {
        return serviceRepository.findActive().stream()
                .map(ServiceDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/services/all")
    @PreAuthorize("hasAnyRole('CLERK', 'OWNER')")
    public List<ServiceDto> listAll() {
        return serviceCatalogService.listAll();
    }

    @PostMapping("/services")
    @PreAuthorize("hasAnyRole('CLERK', 'OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceDto create(@Valid @RequestBody ServiceRequest request) {
        return serviceCatalogService.create(request);
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('CLERK', 'OWNER')")
    public ServiceDto update(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        return serviceCatalogService.update(id, request);
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('CLERK', 'OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        serviceCatalogService.deactivate(id);
    }
}
