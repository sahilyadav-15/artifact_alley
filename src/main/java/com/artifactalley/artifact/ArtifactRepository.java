package com.artifactalley.artifact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    List<Artifact> findByStatusOrderByClosesAtAsc(ArtifactStatus status);

    List<Artifact> findByStatusOrderByIdAsc(ArtifactStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select artifact from Artifact artifact where artifact.id = :id")
    Optional<Artifact> findByIdForUpdate(@Param("id") Long id);
}
