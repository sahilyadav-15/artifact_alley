package com.artifactalley.report;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.math.BigDecimal;
import java.util.List;

@JacksonXmlRootElement(localName = "auctionSummary")
public record AuctionReport(
        @JacksonXmlElementWrapper(localName = "artifactCounts")
        @JacksonXmlProperty(localName = "statusCount") List<StatusCount> artifactCounts,
        long totalBids, BigDecimal totalSoldValue, BigDecimal averageWinningBid,
        @JacksonXmlElementWrapper(localName = "activeAuctions")
        @JacksonXmlProperty(localName = "auction") List<ActiveAuction> activeAuctions,
        @JacksonXmlElementWrapper(localName = "sellerListings")
        @JacksonXmlProperty(localName = "seller") List<ParticipantCount> sellerListings,
        @JacksonXmlElementWrapper(localName = "bidderParticipation")
        @JacksonXmlProperty(localName = "bidder") List<ParticipantCount> bidderParticipation) {
    public record StatusCount(String status, long count) { }
    public record ActiveAuction(Long artifactId, String title, long bidCount) { }
    public record ParticipantCount(Long userId, String displayName, long count) { }
}
