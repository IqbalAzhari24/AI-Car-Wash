package com.carwash.backend.repository;

import com.carwash.backend.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {

    @Query("SELECT s FROM Service s WHERE s.isActive = true ORDER BY s.price")
    List<Service> findActive();
}
