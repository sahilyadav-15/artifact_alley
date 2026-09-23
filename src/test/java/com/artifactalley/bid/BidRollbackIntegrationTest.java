package com.artifactalley.bid;

import com.artifactalley.artifact.*;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(BidRollbackIntegrationTest.RollbackConfiguration.class)
class BidRollbackIntegrationTest {
    @Autowired ArtifactRepository artifacts;
    @Autowired BidRepository bids;
    @Autowired UserRepository users;
    @Autowired FailingBidWorkflow workflow;

    @Test
    void outerFailureRollsBackBidInsertAndPriceUpdate() {
        String suffix = Long.toHexString(System.nanoTime());
        User seller = users.save(new User("Rollback Seller", "rollback-s-" + suffix + "@test.invalid", "hash", Role.SELLER));
        User bidder = users.save(new User("Rollback Bidder", "rollback-b-" + suffix + "@test.invalid", "hash", Role.BIDDER));
        Artifact artifact = artifacts.save(new Artifact("Rollback fixture", Category.OTHER, "1900",
                new BigDecimal("500"), LocalDateTime.now().plusDays(1), "Rollback integration fixture", seller,
                seller.getEmail(), ArtifactStatus.LIVE, LocalDateTime.now()));

        assertThatThrownBy(() -> workflow.placeThenFail(artifact.getId(), bidder.getId(), new BigDecimal("600")))
                .isInstanceOf(IllegalStateException.class).hasMessage("simulated downstream failure");

        assertThat(bids.countByArtifactId(artifact.getId())).isZero();
        assertThat(artifacts.findById(artifact.getId()).orElseThrow().getCurrentPrice()).isEqualByComparingTo("500.00");
    }

    @TestConfiguration
    static class RollbackConfiguration {
        @Bean FailingBidWorkflow failingBidWorkflow(BiddingService biddingService) {
            return new FailingBidWorkflow(biddingService);
        }
    }

    static class FailingBidWorkflow {
        private final BiddingService biddingService;
        FailingBidWorkflow(BiddingService biddingService) { this.biddingService = biddingService; }
        @Transactional
        public void placeThenFail(Long artifactId, Long bidderId, BigDecimal amount) {
            biddingService.placeBid(artifactId, bidderId, amount);
            throw new IllegalStateException("simulated downstream failure");
        }
    }
}
