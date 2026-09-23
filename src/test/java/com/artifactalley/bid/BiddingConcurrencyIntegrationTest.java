package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.Category;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "artifactalley.auction.minimum-increment=100.00")
class BiddingConcurrencyIntegrationTest {
    @Autowired private BiddingService biddingService;
    @Autowired private BidRepository bidRepository;
    @Autowired private ArtifactRepository artifactRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        bidRepository.deleteAll();
        artifactRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void concurrentBidsCannotBothUseTheSamePreviousPrice() throws Exception {
        Artifact artifact = artifactRepository.save(new Artifact("Concurrent lot", Category.OTHER, "1900",
                new BigDecimal("1000.00"), LocalDateTime.now().plusHours(1), "Description"));
        User bidderOne = userRepository.save(new User("One", "one@example.com", "hash", Role.BIDDER));
        User bidderTwo = userRepository.save(new User("Two", "two@example.com", "hash", Role.BIDDER));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> placeAtSameTime(artifact.getId(), bidderOne.getId(), ready, start));
            Future<Boolean> second = executor.submit(() -> placeAtSameTime(artifact.getId(), bidderTwo.getId(), ready, start));
            ready.await();
            start.countDown();

            long successes = List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count();
            assertEquals(1, successes);
        }

        assertEquals(1, bidRepository.countByArtifactId(artifact.getId()));
        assertEquals(0, new BigDecimal("1100.00").compareTo(artifactRepository.findById(artifact.getId()).orElseThrow().getCurrentPrice()));
    }

    @Test
    void bidHistoryIsNewestFirst() {
        Artifact artifact = artifactRepository.save(new Artifact("History lot", Category.OTHER, "1900",
                new BigDecimal("1000.00"), LocalDateTime.now().plusHours(1), "Description"));
        User bidderOne = userRepository.save(new User("One", "one@example.com", "hash", Role.BIDDER));
        User bidderTwo = userRepository.save(new User("Two", "two@example.com", "hash", Role.BIDDER));

        biddingService.placeBid(artifact.getId(), bidderOne.getId(), new BigDecimal("1100.00"));
        biddingService.placeBid(artifact.getId(), bidderTwo.getId(), new BigDecimal("1200.00"));

        assertEquals(List.of(new BigDecimal("1200.00"), new BigDecimal("1100.00")),
                biddingService.getAuctionDetails(artifact.getId()).getBidHistory().stream()
                        .map(BidHistoryItem::getAmount).toList());
    }

    @Test
    void rejectedBidDoesNotPartiallyChangePriceOrHistory() {
        Artifact artifact = artifactRepository.save(new Artifact("Atomic lot", Category.OTHER, "1900",
                new BigDecimal("1000.00"), LocalDateTime.now().plusHours(1), "Description"));
        User bidder = userRepository.save(new User("Bidder", "bidder@example.com", "hash", Role.BIDDER));

        assertThrows(BidBelowMinimumException.class,
                () -> biddingService.placeBid(artifact.getId(), bidder.getId(), new BigDecimal("1050.00")));

        assertEquals(0, bidRepository.countByArtifactId(artifact.getId()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(
                artifactRepository.findById(artifact.getId()).orElseThrow().getCurrentPrice()));
    }

    private boolean placeAtSameTime(Long artifactId, Long bidderId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            biddingService.placeBid(artifactId, bidderId, new BigDecimal("1100.00"));
            return true;
        } catch (BidBelowMinimumException exception) {
            return false;
        }
    }
}
