package com.artifactalley.artifact;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    List<Artifact> findByStatusOrderByClosesAtAsc(ArtifactStatus status);

    List<Artifact> findByStatusOrderByIdAsc(ArtifactStatus status);
}
