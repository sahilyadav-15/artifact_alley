package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.auction.AuctionSettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BidderActivityService {
    private static final Logger log = LoggerFactory.getLogger(BidderActivityService.class);
    private final BidRepository bidRepository;
    private final AuctionSettlementService settlementService;
    private final Clock clock;

    public BidderActivityService(BidRepository bidRepository, AuctionSettlementService settlementService, Clock clock) {
        this.bidRepository = bidRepository;
        this.settlementService = settlementService;
        this.clock = clock;
    }

    public BidderActivity findActivity(Long bidderId) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long artifactId : bidRepository.findExpiredLiveArtifactIdsForBidder(bidderId, now)) {
            try {
                settlementService.settleAuction(artifactId);
            } catch (RuntimeException exception) {
                log.error("Could not settle bidder activity artifact {}: {}", artifactId, exception.getMessage());
            }
        }
        return loadActivity(bidderId, now);
    }

    private BidderActivity loadActivity(Long bidderId, LocalDateTime now) {
        List<BidderActivityEntry> active = new ArrayList<>();
        List<BidderActivityEntry> won = new ArrayList<>();
        List<BidderActivityEntry> ended = new ArrayList<>();
        for (Bid bid : bidRepository.findHighestBidsByBidderWithArtifact(bidderId)) {
            Artifact artifact = bid.getArtifact();
            boolean isActive = artifact.getStatus() == ArtifactStatus.LIVE && artifact.getClosesAt().isAfter(now);
            boolean isWinner = artifact.getStatus() == ArtifactStatus.SOLD
                    && artifact.getWinningBid() != null
                    && artifact.getWinningBid().getId().equals(bid.getId());
            String outcome = isActive ? "In progress" : isWinner ? "Won" : "Ended";
            BidderActivityEntry entry = new BidderActivityEntry(artifact.getId(), artifact.getTitle(), bid.getAmount(),
                    artifact.getCurrentPrice(), artifact.getClosesAt(), artifact.getStatus(), outcome);
            if (isActive) active.add(entry);
            else if (isWinner) won.add(entry);
            else ended.add(entry);
        }
        return new BidderActivity(active, won, ended);
    }
}
