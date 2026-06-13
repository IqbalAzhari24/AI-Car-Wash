package com.carwash.backend.repository;

import com.carwash.backend.entity.ShopClosure;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ShopClosureRepository extends JpaRepository<ShopClosure, UUID> {
    
    /**
     * Optimized: Fetches only closures that are active after a given threshold point.
     */
    List<ShopClosure> findByEndTimeAfter(LocalDateTime threshold);
}