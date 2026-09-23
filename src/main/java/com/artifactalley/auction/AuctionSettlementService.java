package com.artifactalley.auction;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.bid.ArtifactNotFoundException;
import com.artifactalley.bid.Bid;
import com.artifactalley.bid.BidRepository;
import com.artifactalley.security.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuctionSettlementService {
    private final ArtifactRepository artifactRepository;
    private final BidRepository bidRepository;
    private final Clock clock;
    @Autowired(required = false)
    private SecurityAuditService audit;

    public AuctionSettlementService(ArtifactRepository artifactRepository, BidRepository bidRepository, Clock clock) {
        this.artifactRepository = artifactRepository;
        this.bidRepository = bidRepository;
        this.clock = clock;
    }

    @Transactional
    public Artifact settleAuction(Long artifactId) {
        Artifact artifact = artifactRepository.findByIdForUpdate(artifactId)
                .orElseThrow(ArtifactNotFoundException::new);
        if (artifact.getStatus() != ArtifactStatus.LIVE) return artifact;

        LocalDateTime settlementTime = LocalDateTime.now(clock);
        if (artifact.getClosesAt().isAfter(settlementTime)) {
            throw new AuctionNotExpiredException();
        }

        Bid winningBid = bidRepository.findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(artifactId)
                .orElse(null);
        if (winningBid == null) artifact.settleClosed(settlementTime);
        else artifact.settleSold(winningBid, settlementTime);
        if (audit != null) audit.record(winningBid == null ? "AUCTION_CLOSED" : "AUCTION_SOLD",
                null, artifactId, "success");
        return artifactRepository.save(artifact);
    }
}
