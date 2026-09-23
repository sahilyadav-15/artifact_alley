package com.artifactalley.artifact;

import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import com.artifactalley.security.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArtifactReviewService {
    private final ArtifactRepository artifacts;
    private final UserRepository users;
    private final Clock clock;
    @Autowired(required = false)
    private SecurityAuditService audit;

    public ArtifactReviewService(ArtifactRepository artifacts, UserRepository users, Clock clock) {
        this.artifacts = artifacts; this.users = users; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Artifact> pending() { return artifacts.findReviewQueue(ArtifactStatus.PENDING_APPROVAL); }

    @Transactional(readOnly = true)
    public Artifact pendingDetail(Long id) {
        return artifacts.findByIdWithPeople(id).filter(value -> value.getStatus() == ArtifactStatus.PENDING_APPROVAL)
                .orElseThrow(() -> new ArtifactOperationException("Pending listing was not found."));
    }

    @Transactional
    public void approve(Long id, Long administratorId) {
        Artifact artifact = pendingLocked(id);
        validateListing(artifact);
        artifact.approve(administrator(administratorId), LocalDateTime.now(clock));
        if (audit != null) audit.record("ARTIFACT_APPROVED", administratorId, id, "success");
    }

    @Transactional
    public void reject(Long id, Long administratorId, String reason) {
        Artifact artifact = pendingLocked(id);
        artifact.reject(administrator(administratorId), reason, LocalDateTime.now(clock));
        if (audit != null) audit.record("ARTIFACT_REJECTED", administratorId, id, "success");
    }

    private Artifact pendingLocked(Long id) {
        return artifacts.findByIdForUpdate(id)
                .filter(value -> value.getStatus() == ArtifactStatus.PENDING_APPROVAL)
                .orElseThrow(() -> new ArtifactOperationException("That listing is no longer awaiting review."));
    }

    private User administrator(Long id) {
        return users.findById(id).filter(user -> user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new ArtifactOperationException("Administrator access is required."));
    }

    private void validateListing(Artifact artifact) {
        if (artifact.getTitle() == null || artifact.getTitle().isBlank() || artifact.getCategory() == null
                || artifact.getEra() == null || artifact.getEra().isBlank() || artifact.getStartingPrice() == null
                || artifact.getStartingPrice().signum() <= 0 || artifact.getDescription() == null
                || artifact.getDescription().isBlank() || artifact.getClosesAt() == null
                || !artifact.getClosesAt().isAfter(LocalDateTime.now(clock))) {
            throw new ArtifactOperationException("The listing must contain valid details and a future closing time before approval.");
        }
    }
}
