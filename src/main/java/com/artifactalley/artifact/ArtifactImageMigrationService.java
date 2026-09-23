package com.artifactalley.artifact;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@ConditionalOnProperty(name = "artifactalley.images.migration.enabled", havingValue = "true")
public class ArtifactImageMigrationService {
    private final ArtifactImageRepository images;
    private final LocalArtifactImageStorage local;
    private final CloudinaryArtifactImageStorage cloudinary;

    public ArtifactImageMigrationService(ArtifactImageRepository images, CloudinaryArtifactImageStorage cloudinary,
                                         @Value("${artifactalley.images.directory:./uploads/artifacts}") String directory) {
        this.images = images;
        this.cloudinary = cloudinary;
        this.local = new LocalArtifactImageStorage(directory);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean migrate(Long imageId) {
        ArtifactImage image = images.findById(imageId).orElseThrow();
        if (image.getStorageKey().startsWith("cloudinary:")) return false;
        String extension = image.getContentType().equals("image/png") ? "png" : "jpg";
        try {
            byte[] content = local.open(image.getStorageKey()).getInputStream().readAllBytes();
            ArtifactImageStorage.StoredImage stored = cloudinary.store(content, extension, image.getContentType());
            cloudinary.open(stored.storageKey()).getInputStream().close();
            image.migrateStorageKey(stored.storageKey());
            images.saveAndFlush(image);
            return true;
        } catch (IOException exception) {
            throw new ArtifactOperationException("Could not read an image during migration.");
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean localSourceExists(Long imageId) {
        ArtifactImage image = images.findById(imageId).orElseThrow();
        if (image.getStorageKey().startsWith("cloudinary:")) return true;
        return local.open(image.getStorageKey()).exists();
    }
}
