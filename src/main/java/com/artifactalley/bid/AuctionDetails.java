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

    public AuctionDetails(Artifact artifact, BigDecimal minimumNextBid, List<BidHistoryItem> bidHistory) {
        this.artifact = artifact;
        this.minimumNextBid = minimumNextBid;
        this.bidHistory = List.copyOf(bidHistory);
    }

    public Artifact getArtifact() { return artifact; }
    public BigDecimal getMinimumNextBid() { return minimumNextBid; }
    public List<BidHistoryItem> getBidHistory() { return bidHistory; }
    public int getBidCount() { return bidHistory.size(); }
    public String getClosesAtDisplay() { return artifact.getClosesAt().format(DISPLAY_TIME); }
}
