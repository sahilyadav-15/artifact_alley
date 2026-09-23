package com.artifactalley.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({LoginThrottleService.class, LoginThrottleServiceTest.ClockConfiguration.class})
@TestPropertySource(properties = {"artifactalley.security.login-max-attempts=5", "artifactalley.security.login-window-minutes=15"})
class LoginThrottleServiceTest {
    @Autowired LoginThrottleService throttle;
    @Autowired LoginAttemptRepository attempts;
    @Autowired MutableClock clock;
    MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        attempts.deleteAll();
        clock.set(Instant.parse("2026-09-23T12:00:00Z"));
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
    }

    @Test
    void thresholdBlocksThenWindowExpiryRestoresAccess() {
        for (int count = 0; count < 5; count++) throttle.recordFailure("person@example.test", request);
        assertThat(throttle.isBlocked("person@example.test", request)).isTrue();
        clock.advance(Duration.ofMinutes(16));
        assertThat(throttle.isBlocked("person@example.test", request)).isFalse();
    }

    @Test
    void normalizedIdentifierSharesStateAndSuccessClearsIt() {
        for (int count = 0; count < 5; count++) throttle.recordFailure(" Person@Example.Test ", request);
        assertThat(throttle.isBlocked("person@example.test", request)).isTrue();
        throttle.recordSuccess("PERSON@EXAMPLE.TEST", request);
        assertThat(throttle.isBlocked("person@example.test", request)).isFalse();
        assertThat(attempts.count()).isZero();
    }

    @Test
    void clientKeySeparatesAttemptsAndLongIdentifiersAreHashedToFixedSize() {
        for (int count = 0; count < 5; count++) throttle.recordFailure("x".repeat(20_000), request);
        MockHttpServletRequest otherClient = new MockHttpServletRequest();
        otherClient.setRemoteAddr("192.0.2.11");
        assertThat(throttle.isBlocked("x".repeat(20_000), otherClient)).isFalse();
        assertThat(attempts.findAll()).allSatisfy(attempt -> {
            try {
                var identity = LoginAttempt.class.getDeclaredField("identityHash");
                identity.setAccessible(true);
                assertThat((String) identity.get(attempt)).hasSize(64);
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
    }

    @Test
    void staleRecordsCanBeCleanedWithoutThreadSleeps() {
        throttle.recordFailure("old@example.test", request);
        clock.advance(Duration.ofMinutes(31));
        assertThat(throttle.removeStale()).isEqualTo(1);
    }

    static class ClockConfiguration {
        @Bean @Primary MutableClock mutableClock() { return new MutableClock(); }
    }

    static final class MutableClock extends Clock {
        private Instant instant = Instant.EPOCH;
        void set(Instant value) { instant = value; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
