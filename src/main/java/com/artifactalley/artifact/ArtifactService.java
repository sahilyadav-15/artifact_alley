package com.artifactalley.artifact;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArtifactService {
    private final ArtifactRepository artifactRepository;

    public ArtifactService(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    public List<Artifact> findLiveArtifacts() {
        return artifactRepository.findByStatusOrderByClosesAtAsc(ArtifactStatus.LIVE);
    }

    public Artifact submit(String title, Category category, String era, BigDecimal startingPrice,
                           LocalDateTime closesAt, String description, String sellerEmail) {
        Artifact artifact = new Artifact(title.trim(), category, era.trim(), startingPrice, closesAt,
                description.trim(), sellerEmail, ArtifactStatus.PENDING_APPROVAL);
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
