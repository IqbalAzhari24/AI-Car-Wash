package com.carwash.backend.repository;

import com.carwash.backend.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    List<SystemLog> findByLevelOrderByCreatedAtDesc(String level);

    List<SystemLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);
}
