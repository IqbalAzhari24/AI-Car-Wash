package com.carwash.backend.repository;

import com.carwash.backend.entity.ValetRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ValetRequest}.
 */
public interface ValetRequestRepository extends JpaRepository<ValetRequest, UUID> {

    List<ValetRequest> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    List<ValetRequest> findByLocation_IdOrderByCreatedAtDesc(UUID locationId);
}
