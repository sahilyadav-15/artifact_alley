package com.artifactalley.bid;

import com.artifactalley.artifact.ArtifactStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BidderActivityEntry {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private final Long artifactId;
    private final String title;
    private final BigDecimal bidderBid;
    private final BigDecimal auctionPrice;
    private final LocalDateTime closesAt;
    private final ArtifactStatus status;
    private final String outcome;

    public BidderActivityEntry(Long artifactId, String title, BigDecimal bidderBid, BigDecimal auctionPrice,
                               LocalDateTime closesAt, ArtifactStatus status, String outcome) {
        this.artifactId = artifactId;
        this.title = title;
        this.bidderBid = bidderBid;
        this.auctionPrice = auctionPrice;
        this.closesAt = closesAt;
        this.status = status;
        this.outcome = outcome;
    }

    public Long getArtifactId() { return artifactId; }
    public String getTitle() { return title; }
    public BigDecimal getBidderBid() { return bidderBid; }
    public BigDecimal getAuctionPrice() { return auctionPrice; }
    public String getClosesAtDisplay() { return closesAt.format(DISPLAY_TIME); }
    public ArtifactStatus getStatus() { return status; }
    public String getOutcome() { return outcome; }
}
