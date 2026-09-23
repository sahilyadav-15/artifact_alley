package com.artifactalley.api;

import com.artifactalley.artifact.*;
import com.artifactalley.bid.AuctionDetails;
import com.artifactalley.bid.BidRepository;
import com.artifactalley.bid.BiddingService;
import com.artifactalley.bid.ArtifactNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArtifactApiReadService {
    private static final java.util.EnumSet<ArtifactStatus> PUBLIC_STATUSES =
            java.util.EnumSet.of(ArtifactStatus.LIVE, ArtifactStatus.SOLD, ArtifactStatus.CLOSED);
    private final ArtifactRepository artifacts;
    private final BidRepository bids;
    private final ArtifactImageService images;
    private final BiddingService bidding;
    private final ArtifactSearchService search;
    private final Clock clock;

    public ArtifactApiReadService(ArtifactRepository artifacts, BidRepository bids,
                                  ArtifactImageService images, BiddingService bidding, Clock clock,
                                  ArtifactSearchService search) {
        this.artifacts = artifacts;
        this.bids = bids;
        this.images = images;
        this.bidding = bidding;
        this.clock = clock;
        this.search = search;
    }

    @Transactional(readOnly = true)
    public ApiDtos.ArtifactPage publicPage(ArtifactSearchRequest request) {
        ArtifactSearchResult result = search.searchActiveArtifacts(request);
        List<ApiDtos.ArtifactView> content = result.getItems().stream().map(item -> new ApiDtos.ArtifactView(
                item.getId(), item.getTitle(), item.getCategory(), item.getEra(), item.getStartingPrice(),
                item.getCurrentPrice(), item.getClosesAt(), ArtifactStatus.LIVE, item.getDescription(),
                item.getCoverImageUrl(), item.getBidCount(), item.getMinimumNextBid())).toList();
        return new ApiDtos.ArtifactPage(content, result.getCurrentPage(), result.getPageSize(), result.getTotalItems(),
                result.getTotalPages(), result.getSort(), result.getAppliedFilters());
    }

    @Transactional(readOnly = true)
    public ApiDtos.ArtifactView publicArtifact(Long id) {
        Artifact artifact = publicEntity(id);
        return publicView(artifact);
    }

    @Transactional(readOnly = true)
    public ApiDtos.BidHistory publicBids(Long id) {
        publicEntity(id);
        AuctionDetails details = bidding.getAuctionDetails(id);
        List<ApiDtos.BidView> values = details.getBidHistory().stream()
                .map(value -> new ApiDtos.BidView(value.getAmount(), value.getPlacedAt(), value.getBidderDisplayName()))
                .toList();
        return new ApiDtos.BidHistory(id, values, values.size());
    }

    @Transactional(readOnly = true)
    public ApiDtos.SellerArtifactView sellerView(Artifact artifact) {
        List<String> urls = images.list(artifact.getId()).stream().map(images::url).toList();
        return new ApiDtos.SellerArtifactView(artifact.getId(), artifact.getTitle(), artifact.getCategory(),
                artifact.getEra(), artifact.getStartingPrice(), artifact.getCurrentPrice(), artifact.getClosesAt(),
                artifact.getStatus(), artifact.getDescription(), artifact.getSubmittedAt(), artifact.getUpdatedAt(),
                artifact.getRejectionReason(), bids.countByArtifactId(artifact.getId()), urls);
    }

    @Transactional(readOnly = true)
    public ApiDtos.AdminArtifactView adminView(Long id) {
        return adminView(artifacts.findByIdWithPeople(id).orElseThrow(ArtifactNotFoundException::new));
    }

    public ApiDtos.AdminArtifactView adminView(Artifact artifact) {
        return new ApiDtos.AdminArtifactView(artifact.getId(), artifact.getTitle(), artifact.getCategory(),
                artifact.getEra(), artifact.getStartingPrice(), artifact.getClosesAt(), artifact.getStatus(),
                artifact.getDescription(), artifact.getSeller() == null ? null : artifact.getSeller().getId(),
                artifact.getSeller() == null ? null : artifact.getSeller().getName(), artifact.getSubmittedAt(),
                artifact.getReviewedAt(), artifact.getRejectionReason());
    }

    private Artifact publicEntity(Long id) {
        return artifacts.findById(id).filter(value -> PUBLIC_STATUSES.contains(value.getStatus()))
                .orElseThrow(ArtifactNotFoundException::new);
    }

    private ApiDtos.ArtifactView publicView(Artifact artifact) {
        return new ApiDtos.ArtifactView(artifact.getId(), artifact.getTitle(), artifact.getCategory(), artifact.getEra(),
                artifact.getStartingPrice(), artifact.getCurrentPrice(), artifact.getClosesAt(), artifact.getStatus(),
                artifact.getDescription(), images.coverUrl(artifact.getId()), bids.countByArtifactId(artifact.getId()),
                bidding.minimumBidFor(artifact));
    }
}
