package com.carwash.backend.controller;

import com.carwash.backend.dto.LocationDto;
import com.carwash.backend.dto.ServiceDto;
import com.carwash.backend.entity.ShopSetting;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.ServiceRepository;
import com.carwash.backend.repository.ShopSettingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unauthenticated content for the public landing page — active services,
 * branch locations, contact details and operating hours, all read from the DB
 * so nothing on the landing page is hardcoded.
 *
 * <p>Base path {@code /api/v1/public/**} is permitted in WebSecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final ServiceRepository serviceRepository;
    private final LocationRepository locationRepository;
    private final ShopSettingRepository shopSettingRepository;

    public PublicController(ServiceRepository serviceRepository,
                            LocationRepository locationRepository,
                            ShopSettingRepository shopSettingRepository) {
        this.serviceRepository = serviceRepository;
        this.locationRepository = locationRepository;
        this.shopSettingRepository = shopSettingRepository;
    }

    @GetMapping("/landing")
    public Map<String, Object> landing() {
        List<ServiceDto> services = serviceRepository.findActive().stream()
                .map(ServiceDto::from)
                .collect(Collectors.toList());

        List<LocationDto> locations = locationRepository.findActive().stream()
                .map(LocationDto::from)
                .collect(Collectors.toList());

        Map<String, String> settings = shopSettingRepository.findAll().stream()
                .collect(Collectors.toMap(ShopSetting::getSettingKey, ShopSetting::getSettingValue));

        Map<String, Object> data = new HashMap<>();
        data.put("services", services);
        data.put("locations", locations);
        data.put("contact", Map.of(
                "phone", settings.getOrDefault("contact_phone", ""),
                "email", settings.getOrDefault("contact_email", "")
        ));
        data.put("fees", Map.of(
                "pickup", settings.getOrDefault("pickup_fee", "5.00"),
                "delivery", settings.getOrDefault("delivery_fee", "5.00")
        ));
        data.put("hours", Map.of(
                "monThu", settings.getOrDefault("hours_mon_thu", ""),
                "fri",    settings.getOrDefault("hours_fri", ""),
                "sat",    settings.getOrDefault("hours_sat", ""),
                "sun",    settings.getOrDefault("hours_sun", "")
        ));

        return Map.of(
                "status", "success",
                "data", data,
                "message", "Landing content retrieved.",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
