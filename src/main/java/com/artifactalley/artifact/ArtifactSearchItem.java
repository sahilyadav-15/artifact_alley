package com.artifactalley.artifact;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ArtifactSearchItem {
    private final Long id;
    private final String title;
    private final Category category;
    private final String era;
    private final BigDecimal startingPrice;
    private final BigDecimal currentPrice;
    private final LocalDateTime closesAt;
    private final String description;
    private final long bidCount;
    private final String coverImageUrl;
    private final BigDecimal minimumNextBid;
    private final String endsIn;

    public ArtifactSearchItem(Long id, String title, Category category, String era, BigDecimal startingPrice,
                              BigDecimal currentPrice, LocalDateTime closesAt, String description, long bidCount,
                              String coverImageUrl, BigDecimal minimumNextBid, String endsIn) {
        this.id = id; this.title = title; this.category = category; this.era = era;
        this.startingPrice = startingPrice; this.currentPrice = currentPrice; this.closesAt = closesAt;
        this.description = description; this.bidCount = bidCount; this.coverImageUrl = coverImageUrl;
        this.minimumNextBid = minimumNextBid; this.endsIn = endsIn;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Category getCategory() { return category; }
    public String getEra() { return era; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public String getDescription() { return description; }
    public long getBidCount() { return bidCount; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public BigDecimal getMinimumNextBid() { return minimumNextBid; }
    public String getEndsIn() { return endsIn; }
}
