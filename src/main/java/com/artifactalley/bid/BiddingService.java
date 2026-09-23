package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import com.artifactalley.security.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
public class BiddingService {
    private final ArtifactRepository artifactRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final BigDecimal minimumIncrement;
    private final Clock clock;
    @Autowired(required = false)
    private SecurityAuditService audit;

    public BiddingService(ArtifactRepository artifactRepository, UserRepository userRepository,
                          BidRepository bidRepository,
                          @Value("${artifactalley.auction.minimum-increment:100.00}") BigDecimal minimumIncrement,
                          Clock clock) {
        if (minimumIncrement == null || minimumIncrement.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The auction minimum increment must be greater than zero.");
        }
        this.artifactRepository = artifactRepository;
        this.userRepository = userRepository;
        this.bidRepository = bidRepository;
        this.minimumIncrement = minimumIncrement;
        this.clock = clock;
    }

    @Transactional
    public Bid placeBid(Long artifactId, Long bidderUserId, BigDecimal amount) {
        Artifact artifact = artifactRepository.findByIdForUpdate(artifactId)
                .orElseThrow(ArtifactNotFoundException::new);
        User bidder = userRepository.findById(bidderUserId)
                .orElseThrow(BidderNotFoundException::new);

        if (bidder.getRole() != Role.BIDDER) {
            throw new BidNotAllowedException("Only bidder accounts can place bids.");
        }
        if (artifact.getStatus() != ArtifactStatus.LIVE) {
            throw new BidNotAllowedException("This auction is not live, so it cannot accept bids.");
        }

        LocalDateTime placedAt = LocalDateTime.now(clock);
        if (!artifact.getClosesAt().isAfter(placedAt)) {
            throw new BidNotAllowedException("This auction has already closed.");
        }
        if ((artifact.getSeller() != null && artifact.getSeller().getId().equals(bidder.getId()))
                || (artifact.getSeller() == null && artifact.getSubmittedByEmail() != null
                && artifact.getSubmittedByEmail().equalsIgnoreCase(bidder.getEmail()))) {
            throw new BidNotAllowedException("You cannot bid on your own artifact.");
        }

        BigDecimal minimumBid = minimumBidFor(artifact);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(minimumBid) < 0) {
            throw new BidBelowMinimumException(minimumBid);
        }

        Bid bid = bidRepository.save(new Bid(artifact, bidder, amount, placedAt));
        artifact.updateCurrentPrice(amount);
        artifactRepository.save(artifact);
        if (audit != null) audit.record("BID_ACCEPTED", bidderUserId, artifactId, "success");
        return bid;
    }

    @Transactional(readOnly = true)
    public AuctionDetails getAuctionDetails(Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(ArtifactNotFoundException::new);
        List<BidHistoryItem> history = bidRepository.findByArtifactIdOrderByPlacedAtDescIdDesc(artifactId).stream()
                .map(bid -> new BidHistoryItem(bid.getAmount(), bid.getPlacedAt(), maskName(bid.getBidder().getName())))
                .toList();
        Bid winningBid = artifact.getWinningBid();
        String winnerDisplayName = winningBid == null ? null : maskName(winningBid.getBidder().getName());
        Long winningBidderId = winningBid == null ? null : winningBid.getBidder().getId();
        return new AuctionDetails(artifact, minimumBidFor(artifact), history, winnerDisplayName, winningBidderId);
    }

    public BigDecimal minimumBidFor(Artifact artifact) {
        return artifact.getCurrentPrice().add(minimumIncrement);
    }

    public BigDecimal getMinimumIncrement() { return minimumIncrement; }

    private String maskName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return "Private bidder";
        return trimmed.substring(0, 1).toUpperCase() + "***";
    }
}
