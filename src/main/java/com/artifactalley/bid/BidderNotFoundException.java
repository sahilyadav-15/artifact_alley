package com.artifactalley.bid;

public class BidderNotFoundException extends BiddingException {
    public BidderNotFoundException() {
        super("Your account could not be found. Please sign in again.");
    }
}
