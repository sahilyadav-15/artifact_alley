package com.artifactalley.artifact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    List<Artifact> findByStatusOrderByClosesAtAsc(ArtifactStatus status);

    List<Artifact> findByStatusOrderByIdAsc(ArtifactStatus status);

    List<Artifact> findByStatusAndClosesAtAfterOrderByClosesAtAsc(ArtifactStatus status, LocalDateTime now);

    @Query("select artifact.id from Artifact artifact where artifact.status = :status "
            + "and artifact.closesAt <= :now order by artifact.closesAt asc, artifact.id asc")
    List<Long> findExpiredIds(@Param("status") ArtifactStatus status, @Param("now") LocalDateTime now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select artifact from Artifact artifact where artifact.id = :id")
    Optional<Artifact> findByIdForUpdate(@Param("id") Long id);

    @Query("select artifact from Artifact artifact left join fetch artifact.seller where artifact.seller.id = :sellerId order by artifact.updatedAt desc, artifact.id desc")
    List<Artifact> findAllOwnedBy(@Param("sellerId") Long sellerId);

    @Query("select artifact from Artifact artifact left join fetch artifact.seller left join fetch artifact.reviewedBy where artifact.id = :id")
    Optional<Artifact> findByIdWithPeople(@Param("id") Long id);

    @Query("select artifact from Artifact artifact left join fetch artifact.seller where artifact.status = :status order by artifact.id asc")
    List<Artifact> findReviewQueue(@Param("status") ArtifactStatus status);

    List<Artifact> findBySellerIsNullAndSubmittedByEmailIsNotNull();
}
