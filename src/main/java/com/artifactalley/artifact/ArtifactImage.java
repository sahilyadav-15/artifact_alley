package com.artifactalley.artifact;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artifact_images")
public class ArtifactImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    @Column(nullable = false, unique = true, length = 100)
    private String storageKey;
    @Column(nullable = false, length = 255)
    private String originalFilename;
    @Column(nullable = false, length = 30)
    private String contentType;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean coverImage;
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    protected ArtifactImage() { }

    public ArtifactImage(Artifact artifact, String storageKey, String originalFilename, String contentType,
                         int displayOrder, boolean coverImage, LocalDateTime uploadedAt) {
        this.artifact = artifact;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.displayOrder = displayOrder;
        this.coverImage = coverImage;
        this.uploadedAt = uploadedAt;
    }

    public void makeCover(boolean cover) { this.coverImage = cover; }
    public void moveTo(int order) { this.displayOrder = order; }
    public Long getId() { return id; }
    public Artifact getArtifact() { return artifact; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isCoverImage() { return coverImage; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
