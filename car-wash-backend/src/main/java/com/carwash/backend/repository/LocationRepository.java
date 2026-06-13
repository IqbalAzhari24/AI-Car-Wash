package com.carwash.backend.repository;

import com.carwash.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query("SELECT l FROM Location l WHERE l.isActive = true ORDER BY l.name")
    List<Location> findActive();
}
