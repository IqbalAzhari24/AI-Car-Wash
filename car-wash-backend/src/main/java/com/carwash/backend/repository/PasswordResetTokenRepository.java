package com.carwash.backend.repository;

import com.carwash.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Finds a token that is unused and not yet expired, matched by its SHA-256 hash.
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.tokenHash = :tokenHash
              AND t.used = false
              AND t.expiresAt > :now
            """)
    Optional<PasswordResetToken> findValidToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );

    /**
     * Marks all open tokens for a given user as used before issuing a new one.
     * Prevents token accumulation.
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
            SET t.used = true, t.updatedAt = :now
            WHERE t.user.id = :userId AND t.used = false
            """)
    int invalidateAllForUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Removes expired tokens older than the given cutoff.
     * Intended for periodic cleanup by the scheduler.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
