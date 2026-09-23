package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids", indexes = {
        @Index(name = "idx_bid_artifact_placed", columnList = "artifact_id,placed_at"),
        @Index(name = "idx_bid_bidder", columnList = "bidder_id")
})
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt;

    protected Bid() { }

    public Bid(Artifact artifact, User bidder, BigDecimal amount, LocalDateTime placedAt) {
        this.artifact = artifact;
        this.bidder = bidder;
        this.amount = amount;
        this.placedAt = placedAt;
    }

    public Long getId() { return id; }
    public Artifact getArtifact() { return artifact; }
    public User getBidder() { return bidder; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getPlacedAt() { return placedAt; }
}
