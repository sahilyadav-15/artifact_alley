package com.artifactalley.artifact;

import com.artifactalley.bid.BidRepository;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import com.artifactalley.security.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerArtifactService {
    private final ArtifactRepository artifacts;
    private final UserRepository users;
    private final BidRepository bids;
    private final ArtifactImageService images;
    private final Clock clock;
    @Autowired(required = false)
    private SecurityAuditService audit;

    public SellerArtifactService(ArtifactRepository artifacts, UserRepository users, BidRepository bids,
                                 ArtifactImageService images, Clock clock) {
        this.artifacts = artifacts;
        this.users = users;
        this.bids = bids;
        this.images = images;
        this.clock = clock;
    }

    @Transactional
    public Artifact submit(ArtifactSubmissionForm form, Long sellerId, List<MultipartFile> uploads) {
        User seller = seller(sellerId);
        LocalDateTime now = LocalDateTime.now(clock);
        Artifact artifact = artifacts.save(new Artifact(form.getTitle().trim(), form.getCategory(), form.getEra().trim(),
                form.getStartingPrice(), form.getClosesAt(), form.getDescription().trim(), seller, seller.getEmail(),
                ArtifactStatus.PENDING_APPROVAL, now));
        artifacts.flush();
        if (uploads != null && uploads.stream().anyMatch(file -> file != null && !file.isEmpty())) {
            images.upload(artifact.getId(), sellerId, uploads);
        }
        return artifact;
    }

    @Transactional(readOnly = true)
    public List<Artifact> findOwned(Long sellerId) {
        seller(sellerId);
        return artifacts.findAllOwnedBy(sellerId);
    }

    @Transactional(readOnly = true)
    public long bidCountForOwned(Long artifactId, Long sellerId) {
        owned(artifactId, sellerId);
        return bids.countByArtifactId(artifactId);
    }

    @Transactional(readOnly = true)
    public SellerArtifactDetails details(Long artifactId, Long sellerId) {
        Artifact artifact = owned(artifactId, sellerId);
        return new SellerArtifactDetails(artifact, bids.countByArtifactId(artifactId));
    }

    @Transactional
    public void edit(Long artifactId, Long sellerId, ArtifactSubmissionForm form) {
        Artifact artifact = ownedLocked(artifactId, sellerId);
        artifact.updateDraftDetails(form.getTitle(), form.getCategory(), form.getEra(), form.getStartingPrice(),
                form.getClosesAt(), form.getDescription(), LocalDateTime.now(clock));
    }

    @Transactional
    public void resubmit(Long artifactId, Long sellerId) {
        Artifact artifact = ownedLocked(artifactId, sellerId);
        if (artifact.getTitle() == null || artifact.getTitle().isBlank() || artifact.getCategory() == null
                || artifact.getEra() == null || artifact.getEra().isBlank() || artifact.getStartingPrice() == null
                || artifact.getStartingPrice().signum() <= 0 || artifact.getDescription() == null
                || artifact.getDescription().isBlank() || artifact.getClosesAt() == null
                || !artifact.getClosesAt().isAfter(LocalDateTime.now(clock))) {
            throw new ArtifactOperationException("Correct the listing details and choose a future closing time before resubmitting.");
        }
        artifact.resubmit(LocalDateTime.now(clock));
    }

    @Transactional
    public void withdraw(Long artifactId, Long sellerId) {
        Artifact artifact = ownedLocked(artifactId, sellerId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (artifact.getStatus() == ArtifactStatus.LIVE) {
            if (!artifact.getClosesAt().isAfter(now)) throw new ArtifactOperationException("An ended auction cannot be withdrawn.");
            if (bids.countByArtifactId(artifactId) > 0) throw new ArtifactOperationException("A live auction with accepted bids cannot be withdrawn.");
        }
        try { artifact.withdraw(now); }
        catch (IllegalStateException exception) { throw new ArtifactOperationException(exception.getMessage()); }
        if (audit != null) audit.record("ARTIFACT_WITHDRAWN", sellerId, artifactId, "success");
    }

    public ArtifactSubmissionForm editForm(Artifact artifact) {
        ArtifactSubmissionForm form = new ArtifactSubmissionForm();
        form.setTitle(artifact.getTitle()); form.setCategory(artifact.getCategory()); form.setEra(artifact.getEra());
        form.setStartingPrice(artifact.getStartingPrice()); form.setClosesAt(artifact.getClosesAt());
        form.setDescription(artifact.getDescription());
        return form;
    }

    private User seller(Long id) {
        return users.findById(id).filter(user -> user.getRole() == Role.SELLER)
                .orElseThrow(() -> new ArtifactOperationException("Seller access is required."));
    }

    private Artifact owned(Long artifactId, Long sellerId) {
        seller(sellerId);
        return artifacts.findByIdWithPeople(artifactId)
                .filter(artifact -> artifact.getSeller() != null && artifact.getSeller().getId().equals(sellerId))
                .orElseThrow(() -> new ArtifactOperationException("Listing was not found."));
    }

    private Artifact ownedLocked(Long artifactId, Long sellerId) {
        seller(sellerId);
        return artifacts.findByIdForUpdate(artifactId)
                .filter(artifact -> artifact.getSeller() != null && artifact.getSeller().getId().equals(sellerId))
                .orElseThrow(() -> new ArtifactOperationException("Listing was not found."));
    }

    public record SellerArtifactDetails(Artifact artifact, long bidCount) {
        public Artifact getArtifact() { return artifact; }
        public long getBidCount() { return bidCount; }
    }
}
