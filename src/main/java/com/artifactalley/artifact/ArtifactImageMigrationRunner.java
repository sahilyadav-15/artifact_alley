package com.artifactalley.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(name = "artifactalley.images.migration.enabled", havingValue = "true")
public class ArtifactImageMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ArtifactImageMigrationRunner.class);
    private final ArtifactImageRepository images;
    private final ArtifactImageMigrationService migration;
    private final String mode;

    public ArtifactImageMigrationRunner(ArtifactImageRepository images, ArtifactImageMigrationService migration,
                                        @Value("${artifactalley.images.migration.mode:inventory}") String mode) {
        this.images = images;
        this.migration = migration;
        this.mode = mode;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mode.equals("inventory") && !mode.equals("migrate")) {
            throw new IllegalStateException("artifactalley.images.migration.mode must be inventory or migrate.");
        }
        AtomicInteger migrated = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        images.findAll().stream().map(ArtifactImage::getId).forEach(id -> {
            try {
                if (mode.equals("inventory")) {
                    if (migration.localSourceExists(id)) skipped.incrementAndGet();
                } else if (migration.migrate(id)) migrated.incrementAndGet(); else skipped.incrementAndGet();
            } catch (RuntimeException exception) {
                failed.incrementAndGet();
                log.error("Image migration failed for imageId={}: {}", id, exception.getMessage());
            }
        });
        log.info("Image migration report mode={} total={} migrated={} verifiedOrSkipped={} missingOrFailed={}",
                mode, migrated.get() + skipped.get() + failed.get(), migrated, skipped, failed);
        if (failed.get() > 0) throw new IllegalStateException("Image migration did not complete; local originals were retained.");
    }
}
