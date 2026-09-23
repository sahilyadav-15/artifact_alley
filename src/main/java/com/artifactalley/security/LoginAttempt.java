package com.artifactalley.security;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", uniqueConstraints = @UniqueConstraint(name = "uk_login_attempt_identity_client",
        columnNames = {"identity_hash", "client_hash"}), indexes = {
        @Index(name = "idx_login_attempt_blocked", columnList = "blocked_until"),
        @Index(name = "idx_login_attempt_last_failed", columnList = "last_failed_at")
})
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_hash", nullable = false, length = 64)
    private String identityHash;

    @Column(name = "client_hash", nullable = false, length = 64)
    private String clientHash;

    @Column(nullable = false)
    private int failedCount;

    @Column(nullable = false)
    private LocalDateTime windowStartedAt;

    @Column(nullable = false)
    private LocalDateTime lastFailedAt;

    private LocalDateTime blockedUntil;

    protected LoginAttempt() { }

    LoginAttempt(String identityHash, String clientHash, LocalDateTime now) {
        this.identityHash = identityHash;
        this.clientHash = clientHash;
        this.windowStartedAt = now;
        this.lastFailedAt = now;
    }

    int getFailedCount() { return failedCount; }
    LocalDateTime getWindowStartedAt() { return windowStartedAt; }
    LocalDateTime getBlockedUntil() { return blockedUntil; }

    void recordFailure(LocalDateTime now, int maximumAttempts, long windowMinutes) {
        if (windowStartedAt.plusMinutes(windowMinutes).isBefore(now)) {
            failedCount = 0;
            windowStartedAt = now;
            blockedUntil = null;
        }
        failedCount++;
        lastFailedAt = now;
        if (failedCount >= maximumAttempts) blockedUntil = now.plusMinutes(windowMinutes);
    }
}
