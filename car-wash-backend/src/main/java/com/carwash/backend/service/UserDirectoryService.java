package com.carwash.backend.service;

import com.carwash.backend.dto.CreateStaffRequest;
import com.carwash.backend.dto.StaffCreatedResponse;
import com.carwash.backend.dto.TransactionDto;
import com.carwash.backend.dto.UserDto;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Payment;
import com.carwash.backend.entity.User;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.PaymentRepository;
import com.carwash.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserDirectoryService {

    private static final String TEMP_PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TEMP_PW_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDirectoryService(UserRepository userRepository,
                                BookingRepository bookingRepository,
                                PaymentRepository paymentRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UserDto> findUsers(List<User.UserRole> roles, String search, Pageable pageable) {
        // The JPQL uses "role IN :roles", so an empty filter must expand to all roles.
        List<User.UserRole> roleFilter = (roles == null || roles.isEmpty())
                ? List.of(User.UserRole.values())
                : roles;

        String searchFilter = StringUtils.hasText(search) ? search.trim() : null;

        return userRepository.search(roleFilter, searchFilter, pageable).map(UserDto::from);
    }

    /**
     * A customer's bookings joined with their payments: one query for the
     * bookings, one batched query for all related payments.
     */
    public List<TransactionDto> getCustomerTransactions(UUID customerId) {
        List<Booking> bookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();
        Map<UUID, Payment> paymentsByBookingId = paymentRepository.findByBooking_IdIn(bookingIds).stream()
                .collect(Collectors.toMap(p -> p.getBooking().getId(), Function.identity(), (a, b) -> a));

        return bookings.stream()
                .map(booking -> toTransactionDto(booking, paymentsByBookingId.get(booking.getId())))
                .toList();
    }

    /**
     * Creates a staff account with a one-time temp password. The plain-text password
     * is returned in the response and never stored — the owner must share it immediately.
     * Allowed roles: CLERK, WORKER, OWNER. CUSTOMER accounts are self-registered only.
     */
    public StaffCreatedResponse createStaff(CreateStaffRequest req) {
        if (!StringUtils.hasText(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        if (req.getRole() == null || req.getRole() == User.UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be one of: CLERK, WORKER, OWNER.");
        }
        if (userRepository.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with that email already exists.");
        }

        String tempPassword = generateTempPassword();

        User user = new User();
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(req.getRole());
        if (StringUtils.hasText(req.getPhoneNumber())) {
            user.setPhoneNumber(req.getPhoneNumber().trim());
        }

        User saved = userRepository.save(user);
        return new StaffCreatedResponse(saved.getId(), saved.getEmail(), saved.getRole().name(), tempPassword);
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PW_LENGTH);
        for (int i = 0; i < TEMP_PW_LENGTH; i++) {
            sb.append(TEMP_PW_CHARS.charAt(RANDOM.nextInt(TEMP_PW_CHARS.length())));
        }
        return sb.toString();
    }

    private TransactionDto toTransactionDto(Booking booking, Payment payment) {
        return new TransactionDto(
                booking.getId(),
                booking.getSlotTime(),
                booking.getVClass() != null ? booking.getVClass().name() : null,
                booking.getStatus() != null ? booking.getStatus().name() : null,
                booking.getCreatedAt(),
                payment != null ? payment.getAmount() : null,
                payment != null ? payment.getPaymentStatus() : null,
                payment != null ? payment.getTransactionId() : null);
    }
}
