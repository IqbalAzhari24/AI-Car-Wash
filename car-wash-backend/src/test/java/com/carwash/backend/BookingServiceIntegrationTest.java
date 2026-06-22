package com.carwash.backend;

import com.carwash.backend.dto.BookingDto;
import com.carwash.backend.dto.CreateBookingRequest;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Location;
import com.carwash.backend.entity.SlotCapacity;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.LocationRepository;
import com.carwash.backend.repository.PaymentRepository;
import com.carwash.backend.repository.ServiceRepository;
import com.carwash.backend.repository.SlotCapacityRepository;
import com.carwash.backend.repository.UserRepository;
import com.carwash.backend.service.BookingService;
import com.carwash.backend.service.ToyyibPayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;
    @Autowired UserRepository userRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired SlotCapacityRepository slotCapacityRepository;
    @Autowired PaymentRepository paymentRepository;

    // ponytail: mock ToyyibPay — no real credentials in test env
    @MockBean ToyyibPayService toyyibPayService;

    Location location;
    com.carwash.backend.entity.Service service;

    @BeforeEach
    void setup() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        slotCapacityRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        location = new Location();
        location.setName("Integration Test Branch");
        location.setAddress("99 Test Ave");
        location = locationRepository.save(location);

        service = new com.carwash.backend.entity.Service();
        service.setName("Basic Wash");
        service.setPrice(new BigDecimal("30.00"));
        service.setDurationMinutes(30);
        service.setVehicleSizeMultiplier(BigDecimal.ONE);
        service = serviceRepository.save(service);
    }

    @AfterEach
    void cleanup() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        slotCapacityRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    void createBooking_happy_path_creates_pending_booking() {
        User customer = savedCustomer("happy@test.local");
        LocalDateTime futureSlot = LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0);
        savedSlot(futureSlot, 5, 0);

        CreateBookingRequest req = req(service.getId(), futureSlot, Booking.VehicleClass.SEDAN, "Toyota Vios");
        BookingDto dto = bookingService.createBooking(customer.getId(), false, req);

        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getCustomerId()).isEqualTo(customer.getId());
    }

    @Test
    void createBooking_rejects_past_slot_time() {
        User customer = savedCustomer("past@test.local");
        LocalDateTime pastSlot = LocalDateTime.now().minusHours(1);

        CreateBookingRequest req = req(service.getId(), pastSlot, Booking.VehicleClass.SEDAN, "Toyota Vios");

        assertThatThrownBy(() -> bookingService.createBooking(customer.getId(), false, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot book a slot in the past");
    }

    @Test
    void createBooking_throws_409_when_slot_is_full() {
        User c1 = savedCustomer("c1@test.local");
        User c2 = savedCustomer("c2@test.local");
        LocalDateTime futureSlot = LocalDateTime.now().plusHours(4).withMinute(0).withSecond(0).withNano(0);
        savedSlot(futureSlot, 1, 1); // maxLimit=1, already full

        CreateBookingRequest req = req(service.getId(), futureSlot, Booking.VehicleClass.SEDAN, "Honda City");

        assertThatThrownBy(() -> bookingService.createBooking(c2.getId(), false, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .hasToString("409 CONFLICT");
    }

    /**
     * Critical concurrency test: 5 threads race for a 3-capacity slot.
     * Exactly 3 should succeed; 2 should receive 409 CONFLICT.
     * The PostgreSQL UPDATE guard (`bookedCount < maxLimit`) serialises concurrent attempts
     * at the DB row level — no application-level locking required.
     */
    @Test
    void createBooking_concurrent_reservation_does_not_oversubscribe() throws InterruptedException {
        int capacity = 3;
        int threads  = 5;
        LocalDateTime futureSlot = LocalDateTime.now().plusHours(5).withMinute(0).withSecond(0).withNano(0);
        savedSlot(futureSlot, capacity, 0);

        List<User> customers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            customers.add(savedCustomer("concurrent" + i + "@test.local"));
        }

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go    = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService exec = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final UUID customerId = customers.get(i).getId();
            exec.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    CreateBookingRequest r = req(service.getId(), futureSlot, Booking.VehicleClass.SEDAN, "Toyota Vios");
                    bookingService.createBooking(customerId, false, r);
                    successes.incrementAndGet();
                } catch (ResponseStatusException e) {
                    if (e.getStatusCode() == HttpStatus.CONFLICT) conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        go.countDown();
        exec.shutdown();
        exec.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(successes.get()).isEqualTo(capacity);
        assertThat(conflicts.get()).isEqualTo(threads - capacity);
    }

    // --- helpers ---

    private User savedCustomer(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$placeholder");
        u.setRole(User.UserRole.CUSTOMER);
        return userRepository.save(u);
    }

    private void savedSlot(LocalDateTime time, int maxLimit, int bookedCount) {
        SlotCapacity sc = new SlotCapacity();
        sc.setLocation(location);
        sc.setSlotTime(time);
        sc.setMaxLimit(maxLimit);
        sc.setBookedCount(bookedCount);
        slotCapacityRepository.save(sc);
    }

    private CreateBookingRequest req(UUID serviceId, LocalDateTime slotTime,
                                     Booking.VehicleClass vehicleClass, String vehicleModel) {
        CreateBookingRequest r = new CreateBookingRequest();
        r.setServiceId(serviceId);
        r.setSlotTime(slotTime);
        r.setVehicleClass(vehicleClass);
        r.setVehicleModel(vehicleModel);
        return r;
    }
}
