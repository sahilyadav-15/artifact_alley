package com.artifactalley.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginAttempt> findByIdentityHashAndClientHash(String identityHash, String clientHash);

    void deleteByIdentityHashAndClientHash(String identityHash, String clientHash);

    @Modifying
    @Query("delete from LoginAttempt attempt where attempt.lastFailedAt < :cutoff")
    int deleteStale(@Param("cutoff") LocalDateTime cutoff);
}
