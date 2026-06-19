package com.carwash.backend.controller;

import com.carwash.backend.dto.ServiceDto;
import com.carwash.backend.repository.ServiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only service catalogue, used by the booking flow to populate the
 * service picker and resolve serviceId for {@code POST /bookings}.
 */
@RestController
@RequestMapping("/api/v1")
public class ServiceController {

    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("/services")
    public List<ServiceDto> listActive() {
        return serviceRepository.findActive().stream()
                .map(ServiceDto::from)
                .collect(Collectors.toList());
    }
}
