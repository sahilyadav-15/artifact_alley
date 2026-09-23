package com.artifactalley.bid;

import java.math.BigDecimal;

public class BidBelowMinimumException extends BiddingException {
    private final BigDecimal minimumBid;

    public BidBelowMinimumException(BigDecimal minimumBid) {
        super("The bid must be at least ₹" + minimumBid.toPlainString()
                + ". Another bidder may have raised the price; review the latest bid and try again.");
        this.minimumBid = minimumBid;
    }

    public BigDecimal getMinimumBid() { return minimumBid; }
}
