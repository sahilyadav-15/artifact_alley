package com.artifactalley;

import com.artifactalley.artifact.*;
import com.artifactalley.bid.*;
import com.artifactalley.report.AuctionReportJdbcRepository;
import com.artifactalley.security.LoginAttemptRepository;
import com.artifactalley.security.LoginThrottleService;
import com.artifactalley.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "artifactalley.auction.settlement-initial-delay-ms=3600000",
        "artifactalley.auction.settlement-interval-ms=3600000"
})
class PostgreSqlCompatibilityIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired UserRepository users;
    @Autowired ArtifactRepository artifacts;
    @Autowired BidRepository bids;
    @Autowired BiddingService bidding;
    @Autowired SellerArtifactService sellers;
    @Autowired AuctionReportJdbcRepository reports;
    @Autowired ArtifactSearchService search;
    @Autowired LoginThrottleService throttle;
    @Autowired LoginAttemptRepository attempts;
    @Autowired Flyway flyway;

    @BeforeEach
    void clean() {
        bids.deleteAll(); artifacts.deleteAll(); users.deleteAll(); attempts.deleteAll();
    }

    @Test
    void schemaSearchJdbcReportsAndPersistentThrottleUsePostgreSql() {
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1");
        User seller = users.save(new User("PG Seller", "pg-seller@test.invalid", "hash", Role.SELLER));
        artifacts.save(new Artifact("100% Bronze_Test", Category.SCULPTURE, "Unicode भारत", new BigDecimal("500"),
                LocalDateTime.now().plusDays(2), "Bound query fixture", seller, seller.getEmail(),
                ArtifactStatus.LIVE, LocalDateTime.now()));

        ArtifactSearchResult result = search.searchActiveArtifacts(new ArtifactSearchRequest("100% Bronze_", "SCULPTURE",
                "भारत", "400", "600", "3d", "ending-soon", "1", "12"));
        assertThat(result.getItems()).hasSize(1);
        assertThat(reports.summary(5).activeAuctions()).hasSize(1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.50");
        for (int i = 0; i < 5; i++) throttle.recordFailure("PG-USER@Test.Invalid", request);
        assertThat(throttle.isBlocked("pg-user@test.invalid", request)).isTrue();
        assertThat(attempts.count()).isEqualTo(1);
    }

    @Test
    void rowLocksSerializeBidsAndWithdrawalRaceOnPostgreSql() throws Exception {
        User seller = users.save(new User("PG Race Seller", "pg-race-seller@test.invalid", "hash", Role.SELLER));
        User first = users.save(new User("PG First", "pg-first@test.invalid", "hash", Role.BIDDER));
        User second = users.save(new User("PG Second", "pg-second@test.invalid", "hash", Role.BIDDER));
        Artifact lot = artifacts.save(new Artifact("PG locked lot", Category.OTHER, "1900", new BigDecimal("1000"),
                LocalDateTime.now().plusHours(1), "Lock fixture", seller, seller.getEmail(),
                ArtifactStatus.LIVE, LocalDateTime.now()));
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = executor.submit(() -> simultaneousBid(lot.getId(), first.getId(), ready, start));
            Future<Boolean> b = executor.submit(() -> simultaneousBid(lot.getId(), second.getId(), ready, start));
            ready.await(); start.countDown();
            assertThat(List.of(a.get(), b.get()).stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        }
        assertThat(bids.countByArtifactId(lot.getId())).isEqualTo(1);

        Artifact race = artifacts.save(new Artifact("PG withdrawal race", Category.OTHER, "1900", new BigDecimal("100"),
                LocalDateTime.now().plusHours(1), "Race fixture", seller, seller.getEmail(),
                ArtifactStatus.LIVE, LocalDateTime.now()));
        CountDownLatch raceReady = new CountDownLatch(2), raceStart = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> bid = executor.submit(() -> simultaneous(raceReady, raceStart,
                    () -> bidding.placeBid(race.getId(), first.getId(), new BigDecimal("200"))));
            Future<Throwable> withdrawal = executor.submit(() -> simultaneous(raceReady, raceStart,
                    () -> sellers.withdraw(race.getId(), seller.getId())));
            raceReady.await(); raceStart.countDown(); bid.get(); withdrawal.get();
        }
        Artifact persisted = artifacts.findById(race.getId()).orElseThrow();
        assertThat(persisted.getStatus() == ArtifactStatus.WITHDRAWN && bids.countByArtifactId(race.getId()) > 0).isFalse();
    }

    private boolean simultaneousBid(Long artifactId, Long bidderId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown(); start.await();
        try { bidding.placeBid(artifactId, bidderId, new BigDecimal("1100")); return true; }
        catch (BidBelowMinimumException exception) { return false; }
    }

    private Throwable simultaneous(CountDownLatch ready, CountDownLatch start, Runnable action) {
        ready.countDown();
        try { start.await(); action.run(); return null; }
        catch (Throwable failure) { return failure; }
    }
}
