package com.artifactalley.bid;

import java.util.List;

public final class BidderActivity {
    private final List<BidderActivityEntry> active;
    private final List<BidderActivityEntry> won;
    private final List<BidderActivityEntry> ended;

    public BidderActivity(List<BidderActivityEntry> active, List<BidderActivityEntry> won,
                          List<BidderActivityEntry> ended) {
        this.active = List.copyOf(active);
        this.won = List.copyOf(won);
        this.ended = List.copyOf(ended);
    }

    public List<BidderActivityEntry> getActive() { return active; }
    public List<BidderActivityEntry> getWon() { return won; }
    public List<BidderActivityEntry> getEnded() { return ended; }
}
