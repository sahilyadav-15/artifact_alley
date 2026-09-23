package com.artifactalley.bid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BidHistoryItem {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a");
    private final BigDecimal amount;
    private final LocalDateTime placedAt;
    private final String bidderDisplayName;

    public BidHistoryItem(BigDecimal amount, LocalDateTime placedAt, String bidderDisplayName) {
        this.amount = amount;
        this.placedAt = placedAt;
        this.bidderDisplayName = bidderDisplayName;
    }

    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public String getBidderDisplayName() { return bidderDisplayName; }
    public String getPlacedAtDisplay() { return placedAt.format(DISPLAY_TIME); }
}
