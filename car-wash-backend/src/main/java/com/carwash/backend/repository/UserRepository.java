package com.carwash.backend.repository;

import com.carwash.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Paged directory search. Both filters are optional:
     * - {@code roles} null/empty matches any role;
     * - {@code search} null/blank matches everyone, otherwise matches email or phone (case-insensitive on email).
     */
    @Query("SELECT u FROM User u WHERE (:roles IS NULL OR COALESCE(:roles) IS NULL OR u.role IN :roles) " +
       "AND (:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
       "OR u.phoneNumber LIKE CONCAT('%', CAST(:search AS string), '%'))")
Page<User> search(@Param("roles") List<User.UserRole> roles,
                  @Param("search") String search,
                  Pageable pageable);
}
