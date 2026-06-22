package com.carwash.backend;

import com.carwash.backend.dto.SalesSummaryDto;
import com.carwash.backend.service.OwnerAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OwnerAnalyticsControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    @MockBean OwnerAnalyticsService analyticsService;

    @Test
    void unauthenticated_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/owner/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "CUSTOMER")
    void customer_role_returns_403() throws Exception {
        mockMvc.perform(get("/api/v1/owner/analytics/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "WORKER")
    void worker_role_returns_403() throws Exception {
        mockMvc.perform(get("/api/v1/owner/analytics/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000004", roles = "OWNER")
    void owner_role_returns_200_with_summary() throws Exception {
        SalesSummaryDto summary = new SalesSummaryDto();
        summary.setDate(LocalDate.now());
        summary.setTotalRevenue(new BigDecimal("250.00"));
        summary.setTotalBookings(5L);
        when(analyticsService.getSummary(any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/owner/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(5));
    }
}
