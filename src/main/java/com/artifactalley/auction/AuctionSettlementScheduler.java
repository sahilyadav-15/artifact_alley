package com.artifactalley.auction;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionSettlementScheduler {
    private final AuctionSettlementBatch settlementBatch;

    public AuctionSettlementScheduler(AuctionSettlementBatch settlementBatch) {
        this.settlementBatch = settlementBatch;
    }

    @Scheduled(fixedDelayString = "${artifactalley.auction.settlement-interval-ms:60000}",
            initialDelayString = "${artifactalley.auction.settlement-initial-delay-ms:60000}")
    public void settleExpiredAuctions() {
        settlementBatch.settleExpiredBatch();
    }
}
