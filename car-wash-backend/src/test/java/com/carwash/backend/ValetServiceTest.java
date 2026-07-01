package com.carwash.backend;

import com.carwash.backend.dto.CreateValetRequestDto;
import com.carwash.backend.dto.ValetRequestDto;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.User;
import com.carwash.backend.entity.ValetRequest;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.UserRepository;
import com.carwash.backend.repository.ValetRequestRepository;
import com.carwash.backend.service.ValetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValetServiceTest {

    @Mock ValetRequestRepository valetRepository;
    @Mock LocationRepository locationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ValetService service;

    private UUID customerId;
    private UUID locationId;
    private User customer;
    private Location locationWithCoords;

    // Timah Wash - Main branch coords (KL city centre, from V13 backfill)
    private static final double BRANCH_LAT = 3.1390;
    private static final double BRANCH_LNG = 101.6869;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        locationId = UUID.randomUUID();

        customer = new User();
        customer.setId(customerId);
        customer.setEmail("customer@test.com");
        customer.setRole(User.UserRole.CUSTOMER);

        locationWithCoords = new Location();
        locationWithCoords.setId(locationId);
        locationWithCoords.setName("Timah Wash - Main");
        locationWithCoords.setLatitude(BRANCH_LAT);
        locationWithCoords.setLongitude(BRANCH_LNG);
    }

    // -------------------------------------------------------------------------
    // submitRequest — within radius → ACCEPTED
    // -------------------------------------------------------------------------

    @Test
    void submitRequest_accepted_when_customer_is_within_default_radius() {
        // Customer is ~0.5 km from branch — well within 10 km default
        double nearLat = 3.1430;
        double nearLng = 101.6900;

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(locationWithCoords));
        when(valetRepository.save(any(ValetRequest.class))).thenAnswer(inv -> {
            ValetRequest r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ValetRequestDto result = service.submitRequest(
                customerId.toString(), request(locationId, nearLat, nearLng));

        assertThat(result.getStatus()).isEqualTo(ValetRequest.ValetStatus.ACCEPTED);
        assertThat(result.isWithinRadius()).isTrue();
        assertThat(result.getDistanceKm()).isLessThan(10.0);

        ArgumentCaptor<ValetRequest> captor = ArgumentCaptor.forClass(ValetRequest.class);
        verify(valetRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ValetRequest.ValetStatus.ACCEPTED);
    }

    // -------------------------------------------------------------------------
    // submitRequest — outside radius → REJECTED
    // -------------------------------------------------------------------------

    @Test
    void submitRequest_rejected_when_customer_is_outside_default_radius() {
        // Penang coords — ~320 km from KL branch, far outside 10 km
        double farLat = 5.4141;
        double farLng = 100.3288;

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(locationWithCoords));
        when(valetRepository.save(any(ValetRequest.class))).thenAnswer(inv -> {
            ValetRequest r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ValetRequestDto result = service.submitRequest(
                customerId.toString(), request(locationId, farLat, farLng));

        assertThat(result.getStatus()).isEqualTo(ValetRequest.ValetStatus.REJECTED);
        assertThat(result.isWithinRadius()).isFalse();
        assertThat(result.getDistanceKm()).isGreaterThan(10.0);

        ArgumentCaptor<ValetRequest> captor = ArgumentCaptor.forClass(ValetRequest.class);
        verify(valetRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ValetRequest.ValetStatus.REJECTED);
    }

    // -------------------------------------------------------------------------
    // submitRequest — branch missing GPS coords → IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    void submitRequest_throws_when_branch_has_no_coordinates() {
        Location noCoords = new Location();
        noCoords.setId(locationId);
        noCoords.setName("New Branch");
        noCoords.setLatitude(null);
        noCoords.setLongitude(null);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(noCoords));

        assertThatThrownBy(() ->
                service.submitRequest(customerId.toString(), request(locationId, 3.14, 101.69)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GPS coordinates");

        verify(valetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // submitRequest — unknown customer → IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void submitRequest_throws_when_customer_not_found() {
        when(userRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.submitRequest(customerId.toString(), request(locationId, 3.14, 101.69)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found");

        verify(valetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // submitRequest — unknown location → IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void submitRequest_throws_when_location_not_found() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.submitRequest(customerId.toString(), request(locationId, 3.14, 101.69)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Location not found");

        verify(valetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getMyRequests — returns list for customer
    // -------------------------------------------------------------------------

    @Test
    void getMyRequests_returns_all_requests_for_customer() {
        ValetRequest r1 = savedRequest(customerId, locationId, ValetRequest.ValetStatus.ACCEPTED);
        ValetRequest r2 = savedRequest(customerId, locationId, ValetRequest.ValetStatus.REJECTED);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(valetRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(r1, r2));

        List<ValetRequestDto> results = service.getMyRequests(customerId.toString());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getStatus()).isEqualTo(ValetRequest.ValetStatus.ACCEPTED);
        assertThat(results.get(1).getStatus()).isEqualTo(ValetRequest.ValetStatus.REJECTED);
    }

    // -------------------------------------------------------------------------
    // getRequestsForLocation — returns list for branch (clerk/owner)
    // -------------------------------------------------------------------------

    @Test
    void getRequestsForLocation_returns_all_requests_for_branch() {
        ValetRequest r1 = savedRequest(customerId, locationId, ValetRequest.ValetStatus.ACCEPTED);

        when(valetRepository.findByLocation_IdOrderByCreatedAtDesc(locationId))
                .thenReturn(List.of(r1));

        List<ValetRequestDto> results = service.getRequestsForLocation(locationId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLocationId()).isEqualTo(locationId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CreateValetRequestDto request(UUID locationId, double lat, double lng) {
        CreateValetRequestDto dto = new CreateValetRequestDto();
        dto.setLocationId(locationId.toString());
        dto.setCustomerLat(lat);
        dto.setCustomerLng(lng);
        dto.setCustomerAddress("Test Address");
        dto.setPickupTime(LocalDateTime.now().plusHours(1));
        dto.setVehicleClass(Booking.VehicleClass.SEDAN);
        dto.setVehicleModel("Proton Saga");
        return dto;
    }

    private ValetRequest savedRequest(UUID custId, UUID locId, ValetRequest.ValetStatus status) {
        User c = new User();
        c.setId(custId);
        c.setEmail("customer@test.com");

        Location l = new Location();
        l.setId(locId);
        l.setName("Timah Wash - Main");
        l.setLatitude(BRANCH_LAT);
        l.setLongitude(BRANCH_LNG);

        ValetRequest r = new ValetRequest();
        r.setId(UUID.randomUUID());
        r.setCustomer(c);
        r.setLocation(l);
        r.setCustomerLat(3.14);
        r.setCustomerLng(101.69);
        r.setDistanceKm(0.5);
        r.setRadiusKm(10.0);
        r.setPickupTime(LocalDateTime.now().plusHours(1));
        r.setVehicleClass(Booking.VehicleClass.SEDAN);
        r.setVehicleModel("Proton Saga");
        r.setStatus(status);
        return r;
    }
}
