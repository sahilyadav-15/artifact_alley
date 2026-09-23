package com.artifactalley.auction;

import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.ArtifactStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionSettlementBatch {
    private static final Logger log = LoggerFactory.getLogger(AuctionSettlementBatch.class);
    private final ArtifactRepository artifactRepository;
    private final AuctionSettlementService settlementService;
    private final Clock clock;
    private final int batchSize;

    public AuctionSettlementBatch(ArtifactRepository artifactRepository,
                                  AuctionSettlementService settlementService,
                                  Clock clock,
                                  @Value("${artifactalley.auction.settlement-batch-size:50}") int batchSize) {
        this.artifactRepository = artifactRepository;
        this.settlementService = settlementService;
        this.clock = clock;
        this.batchSize = Math.max(1, batchSize);
    }

    public int settleExpiredBatch() {
        List<Long> artifactIds = artifactRepository.findExpiredIds(ArtifactStatus.LIVE,
                LocalDateTime.now(clock), PageRequest.of(0, batchSize));
        int settled = 0;
        for (Long artifactId : artifactIds) {
            try {
                settlementService.settleAuction(artifactId);
                settled++;
            } catch (RuntimeException exception) {
                log.error("Could not settle expired artifact {}: {}", artifactId, exception.getMessage());
            }
        }
        return settled;
    }

    public int getBatchSize() { return batchSize; }
}
