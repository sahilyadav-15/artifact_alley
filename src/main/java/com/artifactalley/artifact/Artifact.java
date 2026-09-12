package com.artifactalley.artifact;

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

    protected Artifact() { }

    public Artifact(String title, Category category, String era, BigDecimal startingPrice,
                    LocalDateTime closesAt, String description) {
        this(title, category, era, startingPrice, closesAt, description, null, ArtifactStatus.LIVE);
    }

    public Artifact(String title, Category category, String era, BigDecimal startingPrice,
                    LocalDateTime closesAt, String description, String submittedByEmail, ArtifactStatus status) {
        this.title = title;
        this.category = category;
        this.era = era;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.closesAt = closesAt;
        this.description = description;
        this.submittedByEmail = submittedByEmail;
        this.status = status;
    }

    public void approve() {
        if (status != ArtifactStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only pending artifacts can be approved.");
        }
        status = ArtifactStatus.LIVE;
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
}
