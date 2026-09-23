package com.artifactalley.artifact;

import com.artifactalley.bid.BidNotAllowedException;
import com.artifactalley.bid.BidRepository;
import com.artifactalley.bid.BiddingService;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "artifactalley.images.directory=${java.io.tmpdir}/artifactalley-concurrency-images",
        "artifactalley.auction.settlement-initial-delay-ms=3600000",
        "artifactalley.auction.settlement-interval-ms=3600000"
})
class WithdrawalBidConcurrencyIntegrationTest {
    @Autowired UserRepository users;
    @Autowired ArtifactRepository artifacts;
    @Autowired BidRepository bids;
    @Autowired SellerArtifactService sellers;
    @Autowired BiddingService bidding;
    @Autowired Clock clock;

    @Test
    void concurrentFirstBidAndWithdrawalCannotProduceWithdrawnArtifactWithBid() throws Exception {
        User seller = users.save(new User("Race Seller", "race-seller@example.com", "hash", Role.SELLER));
        User bidder = users.save(new User("Race Bidder", "race-bidder@example.com", "hash", Role.BIDDER));
        Artifact artifact = artifacts.save(new Artifact("Race Vase", Category.OTHER, "1900", new BigDecimal("100.00"),
                LocalDateTime.now(clock).plusDays(1), "Concurrency verification", seller, seller.getEmail(),
                ArtifactStatus.LIVE, LocalDateTime.now(clock)));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> bidResult = executor.submit(() -> runTogether(ready, start,
                    () -> bidding.placeBid(artifact.getId(), bidder.getId(), new BigDecimal("200.00"))));
            Future<Throwable> withdrawalResult = executor.submit(() -> runTogether(ready, start,
                    () -> sellers.withdraw(artifact.getId(), seller.getId())));
            ready.await(); start.countDown();
            Throwable bidFailure = bidResult.get();
            Throwable withdrawalFailure = withdrawalResult.get();

            Artifact persisted = artifacts.findById(artifact.getId()).orElseThrow();
            long bidCount = bids.countByArtifactId(artifact.getId());
            assertFalse(persisted.getStatus() == ArtifactStatus.WITHDRAWN && bidCount > 0,
                    "The row lock must prevent a withdrawn listing from accepting a bid.");
            if (persisted.getStatus() == ArtifactStatus.WITHDRAWN) {
                assertEquals(0, bidCount);
                assertInstanceOf(BidNotAllowedException.class, bidFailure);
                assertNull(withdrawalFailure);
            } else {
                assertEquals(ArtifactStatus.LIVE, persisted.getStatus());
                assertEquals(1, bidCount);
                assertNull(bidFailure);
                assertInstanceOf(ArtifactOperationException.class, withdrawalFailure);
            }
        }
    }

    private Throwable runTogether(CountDownLatch ready, CountDownLatch start, Runnable action) {
        ready.countDown();
        try { start.await(); action.run(); return null; }
        catch (Throwable throwable) { return throwable; }
    }
}
