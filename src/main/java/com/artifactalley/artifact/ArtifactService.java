package com.artifactalley.artifact;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
public class ArtifactService {
    private final ArtifactRepository artifactRepository;
    private final Clock clock;

    public ArtifactService(ArtifactRepository artifactRepository, Clock clock) {
        this.artifactRepository = artifactRepository;
        this.clock = clock;
    }

    public List<Artifact> findLiveArtifacts() {
        return artifactRepository.findByStatusAndClosesAtAfterOrderByClosesAtAsc(
                ArtifactStatus.LIVE, LocalDateTime.now(clock));
    }

    public Artifact submit(String title, Category category, String era, BigDecimal startingPrice,
                           LocalDateTime closesAt, String description, String sellerEmail) {
        Artifact artifact = new Artifact(title.trim(), category, era.trim(), startingPrice, closesAt,
                description.trim(), null, sellerEmail, ArtifactStatus.PENDING_APPROVAL, LocalDateTime.now(clock));
        return artifactRepository.save(artifact);
    }

    public List<Artifact> findPendingArtifacts() {
        return artifactRepository.findByStatusOrderByIdAsc(ArtifactStatus.PENDING_APPROVAL);
    }

    public boolean approve(Long id) {
        return artifactRepository.findById(id)
                .filter(artifact -> artifact.getStatus() == ArtifactStatus.PENDING_APPROVAL)
                .map(artifact -> {
                    artifact.approve();
                    artifactRepository.save(artifact);
                    return true;
                })
                .orElse(false);
    }
}
