package com.artifactalley.bid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByArtifactIdOrderByPlacedAtDescIdDesc(Long artifactId);
    long countByArtifactId(Long artifactId);

    Optional<Bid> findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(Long artifactId);

    @Query("select distinct bid.artifact.id from Bid bid where bid.bidder.id = :bidderId "
            + "and bid.artifact.status = com.artifactalley.artifact.ArtifactStatus.LIVE "
            + "and bid.artifact.closesAt <= :now")
    List<Long> findExpiredLiveArtifactIdsForBidder(@Param("bidderId") Long bidderId,
                                                    @Param("now") LocalDateTime now);

    @Query("select bid from Bid bid join fetch bid.artifact artifact "
            + "left join fetch artifact.winningBid winningBid "
            + "where bid.bidder.id = :bidderId and bid.amount = "
            + "(select max(other.amount) from Bid other where other.bidder.id = :bidderId "
            + "and other.artifact.id = artifact.id) order by artifact.closesAt desc, artifact.id desc")
    List<Bid> findHighestBidsByBidderWithArtifact(@Param("bidderId") Long bidderId);
}
