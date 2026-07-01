package com.carwash.backend.controller;

import com.carwash.backend.dto.AnalyticsInsightDto;
import com.carwash.backend.dto.RevenueTrendDto;
import com.carwash.backend.dto.SalesSummaryDto;
import com.carwash.backend.service.OwnerAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Owner-only analytics endpoints.
 * Class-level {@code @PreAuthorize} + the {@code /api/v1/owner/**} rule in
 * WebSecurityConfig provide defence-in-depth access control.
 *
 * <ul>
 *   <li>GET /api/v1/owner/analytics/summary?date=YYYY-MM-DD — daily KPI snapshot</li>
 *   <li>GET /api/v1/owner/analytics/trend?days=7            — revenue trend series</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/owner/analytics")
@PreAuthorize("hasRole('OWNER')")
public class OwnerAnalyticsController {

    private final OwnerAnalyticsService analyticsService;

    public OwnerAnalyticsController(OwnerAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Daily KPI snapshot.
     * Defaults to today when {@code date} is not supplied.
     */
    @GetMapping("/summary")
    public SalesSummaryDto summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.getSummary(date != null ? date : LocalDate.now());
    }

    /**
     * Revenue trend series — one data point per day for the last {@code days} days.
     * Defaults to 7. Clamped to 1–90 by the service layer.
     */
    @GetMapping("/trend")
    public List<RevenueTrendDto> trend(
            @RequestParam(defaultValue = "7") int days) {
        return analyticsService.getTrend(days);
    }

    /**
     * Descriptive-analysis blurb: revenue/bookings vs the trailing 7-day average,
     * plus Malaysia public holiday context. Defaults to today when {@code date} is
     * not supplied.
     */
    @GetMapping("/insight")
    public AnalyticsInsightDto insight(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.getInsight(date != null ? date : LocalDate.now());
    }
}
