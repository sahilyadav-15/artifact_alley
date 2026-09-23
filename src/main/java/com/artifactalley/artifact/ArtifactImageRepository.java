package com.artifactalley.artifact;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ArtifactImageRepository extends JpaRepository<ArtifactImage, Long> {
    List<ArtifactImage> findByArtifactIdOrderByDisplayOrderAscIdAsc(Long artifactId);
    Optional<ArtifactImage> findFirstByArtifactIdAndCoverImageTrue(Long artifactId);
    long countByArtifactId(Long artifactId);
}
