package com.carwash.backend;

import com.carwash.backend.dto.BookingDto;
import com.carwash.backend.dto.CreateBookingRequest;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BookingControllerTest extends AbstractIntegrationTest {

    // BookingController calls auth.getName() → must be a valid UUID
    private static final String CUSTOMER_UUID = "00000000-0000-0000-0000-000000000001";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BookingService bookingService;

    @Test
    void unauthenticated_GET_slots_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/slots").param("date", "2026-07-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = CUSTOMER_UUID, roles = "CUSTOMER")
    void authenticated_GET_slots_returns_200() throws Exception {
        when(bookingService.listSlotsForDate(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/slots").param("date", "2026-07-01"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_POST_booking_returns_401() throws Exception {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setServiceId(UUID.randomUUID());
        req.setSlotTime(LocalDateTime.now().plusHours(1));
        req.setVehicleClass(Booking.VehicleClass.SEDAN);
        req.setVehicleModel("Toyota Vios");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = CUSTOMER_UUID, roles = "CUSTOMER")
    void authenticated_POST_booking_returns_201_on_success() throws Exception {
        BookingDto dto = new BookingDto(
                UUID.randomUUID(),
                UUID.fromString(CUSTOMER_UUID),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Basic Wash",
                LocalDateTime.now().plusHours(2),
                "SEDAN",
                "Toyota Vios",
                "PENDING",
                new BigDecimal("30.00"),
                LocalDateTime.now()
        );
        when(bookingService.createBooking(eq(UUID.fromString(CUSTOMER_UUID)), eq(false), any()))
                .thenReturn(dto);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setServiceId(UUID.randomUUID());
        req.setSlotTime(LocalDateTime.now().plusHours(2));
        req.setVehicleClass(Booking.VehicleClass.SEDAN);
        req.setVehicleModel("Toyota Vios");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
