package com.artifactalley.auction;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.artifact.Category;
import com.artifactalley.bid.Bid;
import com.artifactalley.bid.BidNotAllowedException;
import com.artifactalley.bid.BidRepository;
import com.artifactalley.bid.BidderActivity;
import com.artifactalley.bid.BidderActivityService;
import com.artifactalley.bid.BiddingService;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(AuctionLifecycleIntegrationTest.TestClockConfiguration.class)
class AuctionLifecycleIntegrationTest {
    private static final Instant START = Instant.parse("2026-09-23T08:00:00Z");
    @Autowired private MutableClock clock;
    @Autowired private AuctionSettlementService settlementService;
    @Autowired private BiddingService biddingService;
    @Autowired private BidderActivityService activityService;
    @Autowired private ArtifactRepository artifactRepository;
    @Autowired private BidRepository bidRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        clock.setInstant(START);
        jdbcTemplate.update("update artifacts set winning_bid_id = null");
        bidRepository.deleteAll();
        artifactRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void completeLifecycleSelectsHighestBidAndBuildsWinnerAndLoserActivity() {
        LocalDateTime now = LocalDateTime.now(clock);
        Artifact artifact = artifactRepository.save(liveArtifact("Lifecycle lot", now.plusMinutes(10)));
        Artifact activeArtifact = artifactRepository.save(liveArtifact("Still active", now.plusHours(3)));
        User lowerBidder = userRepository.save(bidder("Lower", "lower@example.com"));
        User winner = userRepository.save(bidder("Winner", "winner@example.com"));
        Bid lowerBid = biddingService.placeBid(artifact.getId(), lowerBidder.getId(), new BigDecimal("1100.00"));
        Bid winningBid = biddingService.placeBid(artifact.getId(), winner.getId(), new BigDecimal("1300.00"));
        biddingService.placeBid(activeArtifact.getId(), winner.getId(), new BigDecimal("1100.00"));

        clock.setInstant(START.plusSeconds(11 * 60));
        Artifact settled = settlementService.settleAuction(artifact.getId());

        assertEquals(ArtifactStatus.SOLD, settled.getStatus());
        assertEquals(winningBid.getId(), settled.getWinningBid().getId());
        assertEquals(winner.getId(), settled.getWinningBid().getBidder().getId());
        assertEquals(new BigDecimal("1300.00"), settled.getCurrentPrice());
        assertNotEquals(lowerBid.getId(), settled.getWinningBid().getId());

        LocalDateTime settledAt = settled.getSettledAt();
        settlementService.settleAuction(artifact.getId());
        Artifact unchanged = artifactRepository.findById(artifact.getId()).orElseThrow();
        assertEquals(winningBid.getId(), unchanged.getWinningBid().getId());
        assertEquals(settledAt, unchanged.getSettledAt());
        assertThrows(BidNotAllowedException.class,
                () -> biddingService.placeBid(artifact.getId(), lowerBidder.getId(), new BigDecimal("1400.00")));

        BidderActivity winnerActivity = activityService.findActivity(winner.getId());
        BidderActivity loserActivity = activityService.findActivity(lowerBidder.getId());
        assertEquals(1, winnerActivity.getWon().size());
        assertEquals(1, winnerActivity.getActive().size());
        assertEquals(1, loserActivity.getEnded().size());
    }

    @Test
    void simultaneousSettlementsCannotCreateConflictingWinners() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        Artifact artifact = artifactRepository.save(liveArtifact("Concurrent settlement", now.plusMinutes(1)));
        User first = userRepository.save(bidder("First", "first@example.com"));
        User second = userRepository.save(bidder("Second", "second@example.com"));
        biddingService.placeBid(artifact.getId(), first.getId(), new BigDecimal("1100.00"));
        Bid highest = biddingService.placeBid(artifact.getId(), second.getId(), new BigDecimal("1200.00"));
        clock.setInstant(START.plusSeconds(120));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Long> one = executor.submit(() -> settleTogether(artifact.getId(), ready, start));
            Future<Long> two = executor.submit(() -> settleTogether(artifact.getId(), ready, start));
            ready.await();
            start.countDown();
            assertEquals(highest.getId(), one.get());
            assertEquals(highest.getId(), two.get());
        }

        Artifact result = artifactRepository.findById(artifact.getId()).orElseThrow();
        assertEquals(ArtifactStatus.SOLD, result.getStatus());
        assertEquals(highest.getId(), result.getWinningBid().getId());
    }

    @Test
    void bidAndSettlementAtClosedBoundaryCannotBothCrossTheRule() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        Artifact artifact = artifactRepository.save(liveArtifact("Boundary lot", now.plusMinutes(1)));
        User first = userRepository.save(bidder("First", "first@example.com"));
        User challenger = userRepository.save(bidder("Challenger", "challenger@example.com"));
        Bid acceptedBeforeClose = biddingService.placeBid(artifact.getId(), first.getId(), new BigDecimal("1100.00"));
        clock.setInstant(START.plusSeconds(60));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> lateBid = executor.submit(() -> {
                ready.countDown(); start.await();
                try {
                    biddingService.placeBid(artifact.getId(), challenger.getId(), new BigDecimal("1200.00"));
                    return true;
                } catch (BidNotAllowedException exception) {
                    return false;
                }
            });
            Future<Long> settlement = executor.submit(() -> settleTogether(artifact.getId(), ready, start));
            ready.await();
            start.countDown();
            assertFalse(lateBid.get());
            assertEquals(acceptedBeforeClose.getId(), settlement.get());
        }

        assertEquals(1, bidRepository.countByArtifactId(artifact.getId()));
        Artifact result = artifactRepository.findById(artifact.getId()).orElseThrow();
        assertEquals(acceptedBeforeClose.getId(), result.getWinningBid().getId());
        assertEquals(new BigDecimal("1100.00"), result.getCurrentPrice());
    }

    @Test
    void expiredSelectionContainsOnlyLiveExpiredRowsAndHonorsLimit() {
        LocalDateTime now = LocalDateTime.now(clock);
        Artifact expiredOne = artifactRepository.save(liveArtifact("Expired one", now.minusMinutes(2)));
        Artifact expiredTwo = artifactRepository.save(liveArtifact("Expired two", now.minusMinutes(1)));
        artifactRepository.save(liveArtifact("Future", now.plusMinutes(1)));
        artifactRepository.save(new Artifact("Pending", Category.OTHER, "1900", new BigDecimal("1000.00"),
                now.minusMinutes(3), "Description", "seller@example.com", ArtifactStatus.PENDING_APPROVAL));

        List<Long> firstPage = artifactRepository.findExpiredIds(ArtifactStatus.LIVE, now, PageRequest.of(0, 1));
        List<Long> allExpired = artifactRepository.findExpiredIds(ArtifactStatus.LIVE, now, PageRequest.of(0, 10));

        assertEquals(List.of(expiredOne.getId()), firstPage);
        assertEquals(List.of(expiredOne.getId(), expiredTwo.getId()), allExpired);
    }

    @Test
    void equalAmountFallbackDeterministicallySelectsTheEarliestAcceptedBid() {
        LocalDateTime now = LocalDateTime.now(clock);
        Artifact artifact = artifactRepository.save(liveArtifact("Legacy tie", now.minusMinutes(1)));
        User earlyBidder = userRepository.save(bidder("Early", "early@example.com"));
        User lateBidder = userRepository.save(bidder("Late", "late@example.com"));
        Bid earliest = bidRepository.save(new Bid(artifact, earlyBidder, new BigDecimal("1200.00"), now.minusMinutes(3)));
        bidRepository.save(new Bid(artifact, lateBidder, new BigDecimal("1200.00"), now.minusMinutes(2)));

        Artifact settled = settlementService.settleAuction(artifact.getId());

        assertEquals(earliest.getId(), settled.getWinningBid().getId());
        assertEquals(earlyBidder.getId(), settled.getWinningBid().getBidder().getId());
    }

    private Long settleTogether(Long artifactId, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return settlementService.settleAuction(artifactId).getWinningBid().getId();
    }

    private Artifact liveArtifact(String title, LocalDateTime closesAt) {
        return new Artifact(title, Category.OTHER, "1900", new BigDecimal("1000.00"), closesAt, "Description");
    }

    private User bidder(String name, String email) {
        return new User(name, email, "hash", Role.BIDDER);
    }

    @TestConfiguration
    static class TestClockConfiguration {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(START, ZoneOffset.UTC);
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = new AtomicReference<>(instant);
            this.zone = zone;
        }

        void setInstant(Instant value) { instant.set(value); }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(instant(), zone); }
        @Override public Instant instant() { return instant.get(); }
    }
}
