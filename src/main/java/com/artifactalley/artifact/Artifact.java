package com.artifactalley.artifact;

import com.artifactalley.bid.Bid;
import com.artifactalley.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "artifacts")
public class Artifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(length = 60)
    private String era;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startingPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private LocalDateTime closesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtifactStatus status;

    @Column(length = 1000)
    private String description;

    @Column(length = 254)
    private String submittedByEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Column
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String rejectionReason;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_bid_id")
    private Bid winningBid;

    @Column
    private LocalDateTime settledAt;

    protected Artifact() { }

    public Artifact(String title, Category category, String era, BigDecimal startingPrice,
                    LocalDateTime closesAt, String description) {
        this(title, category, era, startingPrice, closesAt, description, null, ArtifactStatus.LIVE);
    }

    public Artifact(String title, Category category, String era, BigDecimal startingPrice,
                    LocalDateTime closesAt, String description, String submittedByEmail, ArtifactStatus status) {
        this(title, category, era, startingPrice, closesAt, description, null, submittedByEmail, status,
                LocalDateTime.now());
    }

    public Artifact(String title, Category category, String era, BigDecimal startingPrice,
                    LocalDateTime closesAt, String description, User seller, String submittedByEmail,
                    ArtifactStatus status, LocalDateTime submittedAt) {
        this.title = title;
        this.category = category;
        this.era = era;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.closesAt = closesAt;
        this.description = description;
        this.submittedByEmail = submittedByEmail;
        this.seller = seller;
        this.status = status;
        this.submittedAt = submittedAt;
        this.updatedAt = submittedAt;
    }

    public void approve(User administrator, LocalDateTime reviewedAt) {
        if (status != ArtifactStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only pending artifacts can be approved.");
        }
        status = ArtifactStatus.LIVE;
        reviewedBy = administrator;
        this.reviewedAt = reviewedAt;
        rejectionReason = null;
        updatedAt = reviewedAt;
    }

    public void approve() {
        approve(null, LocalDateTime.now());
    }

    public void reject(User administrator, String reason, LocalDateTime reviewedAt) {
        if (status != ArtifactStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only pending artifacts can be rejected.");
        }
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isBlank() || normalized.length() > 500) {
            throw new IllegalArgumentException("A rejection reason of at most 500 characters is required.");
        }
        status = ArtifactStatus.REJECTED;
        rejectionReason = normalized;
        reviewedBy = administrator;
        this.reviewedAt = reviewedAt;
        updatedAt = reviewedAt;
    }

    public void resubmit(LocalDateTime now) {
        if (status != ArtifactStatus.REJECTED) throw new IllegalStateException("Only rejected artifacts can be resubmitted.");
        status = ArtifactStatus.PENDING_APPROVAL;
        rejectionReason = null;
        reviewedBy = null;
        reviewedAt = null;
        updatedAt = now;
    }

    public void withdraw(LocalDateTime now) {
        if (status != ArtifactStatus.PENDING_APPROVAL && status != ArtifactStatus.REJECTED && status != ArtifactStatus.LIVE) {
            throw new IllegalStateException("This artifact cannot be withdrawn.");
        }
        status = ArtifactStatus.WITHDRAWN;
        updatedAt = now;
    }

    public void updateDraftDetails(String title, Category category, String era, BigDecimal startingPrice,
                                   LocalDateTime closesAt, String description, LocalDateTime now) {
        if (status != ArtifactStatus.PENDING_APPROVAL && status != ArtifactStatus.REJECTED) {
            throw new IllegalStateException("Only pending or rejected artifacts can be edited.");
        }
        this.title = title.trim();
        this.category = category;
        this.era = era.trim();
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.closesAt = closesAt;
        this.description = description.trim();
        this.updatedAt = now;
    }

    public void assignSellerForLegacyRecord(User seller) {
        if (this.seller != null) throw new IllegalStateException("Artifact already has an owner.");
        this.seller = seller;
    }

    public void updateCurrentPrice(BigDecimal amount) {
        this.currentPrice = amount;
    }

    public void settleSold(Bid bid, LocalDateTime settlementTime) {
        if (status != ArtifactStatus.LIVE) {
            throw new IllegalStateException("Only a live auction can be settled.");
        }
        if (bid == null || !sameArtifact(bid.getArtifact())) {
            throw new IllegalArgumentException("The winning bid must belong to this artifact.");
        }
        if (bid.getAmount() == null) {
            throw new IllegalArgumentException("The winning bid must have an amount.");
        }
        status = ArtifactStatus.SOLD;
        winningBid = bid;
        currentPrice = bid.getAmount();
        settledAt = settlementTime;
    }

    public void settleClosed(LocalDateTime settlementTime) {
        if (status != ArtifactStatus.LIVE) {
            throw new IllegalStateException("Only a live auction can be settled.");
        }
        status = ArtifactStatus.CLOSED;
        winningBid = null;
        settledAt = settlementTime;
    }

    private boolean sameArtifact(Artifact other) {
        if (other == this) return true;
        return other != null && id != null && id.equals(other.getId());
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Category getCategory() { return category; }
    public String getEra() { return era; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public ArtifactStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getSubmittedByEmail() { return submittedByEmail; }
    public User getSeller() { return seller; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
    public Bid getWinningBid() { return winningBid; }
    public LocalDateTime getSettledAt() { return settledAt; }
}
