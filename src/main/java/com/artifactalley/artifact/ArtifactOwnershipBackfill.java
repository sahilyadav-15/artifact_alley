package com.artifactalley.artifact;

import com.artifactalley.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class ArtifactOwnershipBackfill implements CommandLineRunner {
    private final ArtifactRepository artifacts;
    private final UserRepository users;

    public ArtifactOwnershipBackfill(ArtifactRepository artifacts, UserRepository users) {
        this.artifacts = artifacts;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(String... args) {
        artifacts.findBySellerIsNullAndSubmittedByEmailIsNotNull().forEach(artifact ->
                users.findByEmailIgnoreCase(artifact.getSubmittedByEmail())
                        .ifPresent(artifact::assignSellerForLegacyRecord));
    }
}
