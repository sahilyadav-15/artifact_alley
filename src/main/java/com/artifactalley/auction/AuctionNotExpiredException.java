package com.artifactalley.auction;

public class AuctionNotExpiredException extends RuntimeException {
    public AuctionNotExpiredException() {
        super("This auction has not reached its closing time.");
    }
}
