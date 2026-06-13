package com.carwash.backend.controller;

import com.carwash.backend.dto.CreateStaffRequest;
import com.carwash.backend.dto.StaffCreatedResponse;
import com.carwash.backend.dto.TransactionDto;
import com.carwash.backend.dto.UserDto;
import com.carwash.backend.entity.User;
import com.carwash.backend.service.UserDirectoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Owner-only Global User Directory. Class-level role gate plus the
 * /api/v1/owner/** rule in WebSecurityConfig — defence in depth.
 */
@RestController
@RequestMapping("/api/v1/owner/users")
@PreAuthorize("hasRole('OWNER')")
public class OwnerUserController {

    private final UserDirectoryService userDirectoryService;

    public OwnerUserController(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    /**
     * Paged, sortable, searchable user list.
     * {@code role} is optional and repeatable: role=CUSTOMER, or
     * role=OWNER&amp;role=CLERK&amp;role=WORKER for the Staff tab.
     * Pagination/sorting come from the standard Pageable params (page, size, sort).
     */
    @GetMapping
    public Page<UserDto> listUsers(
            @RequestParam(name = "role", required = false) List<User.UserRole> role,
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return userDirectoryService.findUsers(role, search, pageable);
    }

    /**
     * A single customer's transaction history (bookings + payments).
     */
    @GetMapping("/{customerId}/transactions")
    public List<TransactionDto> customerTransactions(@PathVariable UUID customerId) {
        return userDirectoryService.getCustomerTransactions(customerId);
    }

    /**
     * Creates a staff account (CLERK, WORKER, or OWNER).
     * Returns a one-time temp password the owner shares with the new staff member.
     * Endpoint: POST /api/v1/owner/users/staff
     */
    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffCreatedResponse createStaff(@RequestBody CreateStaffRequest req) {
        return userDirectoryService.createStaff(req);
    }
}
