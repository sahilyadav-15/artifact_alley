package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class AuctionDetails {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private final Artifact artifact;
    private final BigDecimal minimumNextBid;
    private final List<BidHistoryItem> bidHistory;
    private final String winnerDisplayName;
    private final Long winningBidderId;

    public AuctionDetails(Artifact artifact, BigDecimal minimumNextBid, List<BidHistoryItem> bidHistory) {
        this(artifact, minimumNextBid, bidHistory, null, null);
    }

    public AuctionDetails(Artifact artifact, BigDecimal minimumNextBid, List<BidHistoryItem> bidHistory,
                          String winnerDisplayName, Long winningBidderId) {
        this.artifact = artifact;
        this.minimumNextBid = minimumNextBid;
        this.bidHistory = List.copyOf(bidHistory);
        this.winnerDisplayName = winnerDisplayName;
        this.winningBidderId = winningBidderId;
    }

    public Artifact getArtifact() { return artifact; }
    public BigDecimal getMinimumNextBid() { return minimumNextBid; }
    public List<BidHistoryItem> getBidHistory() { return bidHistory; }
    public int getBidCount() { return bidHistory.size(); }
    public String getClosesAtDisplay() { return artifact.getClosesAt().format(DISPLAY_TIME); }
    public String getWinnerDisplayName() { return winnerDisplayName; }
    public Long getWinningBidderId() { return winningBidderId; }
    public String getSettledAtDisplay() {
        return artifact.getSettledAt() == null ? null : artifact.getSettledAt().format(DISPLAY_TIME);
    }
}
