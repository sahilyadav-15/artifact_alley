package com.artifactalley.bid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByArtifactIdOrderByPlacedAtDescIdDesc(Long artifactId);
    long countByArtifactId(Long artifactId);
}
