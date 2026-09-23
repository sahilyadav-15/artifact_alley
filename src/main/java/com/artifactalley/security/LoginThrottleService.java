package com.artifactalley.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class LoginThrottleService {
    private final LoginAttemptRepository attempts;
    private final Clock clock;
    private final int maximumAttempts;
    private final long windowMinutes;

    public LoginThrottleService(LoginAttemptRepository attempts, Clock clock,
                                @Value("${artifactalley.security.login-max-attempts:5}") int maximumAttempts,
                                @Value("${artifactalley.security.login-window-minutes:15}") long windowMinutes) {
        this.attempts = attempts;
        this.clock = clock;
        this.maximumAttempts = maximumAttempts;
        this.windowMinutes = windowMinutes;
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(String email, HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        return attempts.findByIdentityHashAndClientHash(identity(email), client(request))
                .map(attempt -> attempt.getBlockedUntil() != null && attempt.getBlockedUntil().isAfter(now))
                .orElse(false);
    }

    @Transactional
    public synchronized void recordFailure(String email, HttpServletRequest request) {
        String identity = identity(email);
        String client = client(request);
        LocalDateTime now = LocalDateTime.now(clock);
        LoginAttempt attempt = attempts.findByIdentityHashAndClientHash(identity, client)
                .orElseGet(() -> new LoginAttempt(identity, client, now));
        attempt.recordFailure(now, maximumAttempts, windowMinutes);
        attempts.save(attempt);
    }

    @Transactional
    public void recordSuccess(String email, HttpServletRequest request) {
        attempts.deleteByIdentityHashAndClientHash(identity(email), client(request));
    }

    @Transactional
    public int removeStale() {
        return attempts.deleteStale(LocalDateTime.now(clock).minusMinutes(windowMinutes * 2));
    }

    @Transactional
    @Scheduled(fixedDelayString = "${artifactalley.security.login-cleanup-interval-ms:3600000}")
    public void cleanupStale() {
        attempts.deleteStale(LocalDateTime.now(clock).minusMinutes(windowMinutes * 2));
    }

    private String identity(String email) {
        return digest(email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private String client(HttpServletRequest request) {
        return digest(request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr());
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
