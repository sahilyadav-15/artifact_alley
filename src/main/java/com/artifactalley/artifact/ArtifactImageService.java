package com.artifactalley.artifact;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArtifactImageService {
    public static final long MAX_BYTES = 5L * 1024L * 1024L;
    public static final int MAX_IMAGES = 5;
    private static final long MAX_PIXELS = 25_000_000L;

    private final ArtifactRepository artifacts;
    private final ArtifactImageRepository images;
    private final ArtifactImageStorage storage;
    private final java.time.Clock clock;

    public ArtifactImageService(ArtifactRepository artifacts, ArtifactImageRepository images,
                                ArtifactImageStorage storage, java.time.Clock clock) {
        this.artifacts = artifacts;
        this.images = images;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ArtifactImage> list(Long artifactId) {
        return images.findByArtifactIdOrderByDisplayOrderAscIdAsc(artifactId);
    }

    @Transactional(readOnly = true)
    public String coverUrl(Long artifactId) {
        return images.findFirstByArtifactIdAndCoverImageTrue(artifactId)
                .map(image -> storage.publicUrl(image.getId())).orElse(null);
    }

    public String url(ArtifactImage image) { return storage.publicUrl(image.getId()); }

    @Transactional
    public void upload(Long artifactId, Long sellerId, List<MultipartFile> uploads) {
        Artifact artifact = ownedEditableArtifact(artifactId, sellerId);
        List<PreparedImage> prepared = new ArrayList<>();
        if (uploads != null) {
            for (MultipartFile upload : uploads) {
                if (upload != null && !upload.isEmpty()) prepared.add(prepare(upload));
                else if (upload != null) throw new ArtifactOperationException("Empty image files are not allowed.");
            }
        }
        if (prepared.isEmpty()) throw new ArtifactOperationException("Choose at least one JPEG or PNG image.");
        long currentCount = images.countByArtifactId(artifactId);
        if (currentCount + prepared.size() > MAX_IMAGES) throw new ArtifactOperationException("An artifact may have at most five images.");

        int order = (int) currentCount;
        for (PreparedImage value : prepared) {
            ArtifactImageStorage.StoredImage stored = storage.store(value.bytes(), value.extension(), value.contentType());
            cleanupIfTransactionFails(stored.storageKey());
            images.save(new ArtifactImage(artifact, stored.storageKey(), value.originalFilename(), stored.contentType(),
                    order, order == 0, java.time.LocalDateTime.now(clock)));
            order++;
        }
        images.flush();
    }

    @Transactional
    public void delete(Long artifactId, Long imageId, Long sellerId) {
        ownedEditableArtifact(artifactId, sellerId);
        ArtifactImage image = images.findById(imageId)
                .filter(candidate -> candidate.getArtifact().getId().equals(artifactId))
                .orElseThrow(() -> new ArtifactOperationException("Image was not found."));
        boolean wasCover = image.isCoverImage();
        String key = image.getStorageKey();
        images.delete(image);
        images.flush();
        List<ArtifactImage> remaining = images.findByArtifactIdOrderByDisplayOrderAscIdAsc(artifactId);
        for (int index = 0; index < remaining.size(); index++) {
            ArtifactImage candidate = remaining.get(index);
            candidate.moveTo(index);
            if (wasCover) candidate.makeCover(index == 0);
        }
        afterCommit(() -> storage.delete(key));
    }

    @Transactional
    public void selectCover(Long artifactId, Long imageId, Long sellerId) {
        ownedEditableArtifact(artifactId, sellerId);
        List<ArtifactImage> listingImages = images.findByArtifactIdOrderByDisplayOrderAscIdAsc(artifactId);
        if (listingImages.stream().noneMatch(image -> image.getId().equals(imageId))) {
            throw new ArtifactOperationException("Image was not found.");
        }
        listingImages.forEach(image -> image.makeCover(image.getId().equals(imageId)));
    }

    @Transactional
    public void reorder(Long artifactId, Long sellerId, List<Long> orderedIds) {
        ownedEditableArtifact(artifactId, sellerId);
        List<ArtifactImage> listingImages = images.findByArtifactIdOrderByDisplayOrderAscIdAsc(artifactId);
        if (orderedIds == null || orderedIds.size() != listingImages.size()
                || new HashSet<>(orderedIds).size() != listingImages.size()
                || !orderedIds.containsAll(listingImages.stream().map(ArtifactImage::getId).toList())) {
            throw new ArtifactOperationException("The image order is invalid.");
        }
        for (ArtifactImage image : listingImages) image.moveTo(orderedIds.indexOf(image.getId()));
    }

    @Transactional(readOnly = true)
    public ImageContent open(Long imageId) {
        ArtifactImage image = images.findById(imageId)
                .orElseThrow(() -> new ArtifactOperationException("Image was not found."));
        return new ImageContent(storage.open(image.getStorageKey()), image.getContentType());
    }

    private Artifact ownedEditableArtifact(Long artifactId, Long sellerId) {
        Artifact artifact = artifacts.findByIdForUpdate(artifactId)
                .orElseThrow(() -> new ArtifactOperationException("Listing was not found."));
        if (artifact.getSeller() == null || !artifact.getSeller().getId().equals(sellerId)) {
            throw new ArtifactOperationException("Listing was not found.");
        }
        if (artifact.getStatus() != ArtifactStatus.PENDING_APPROVAL && artifact.getStatus() != ArtifactStatus.REJECTED) {
            throw new ArtifactOperationException("Photos can only be changed while a listing is pending or rejected.");
        }
        return artifact;
    }

    private PreparedImage prepare(MultipartFile upload) {
        if (upload.getSize() > MAX_BYTES) throw new ArtifactOperationException("Each image must be 5 MB or smaller.");
        try {
            byte[] source = upload.getBytes();
            BufferedImage decoded;
            String actualFormat;
            try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(source))) {
                if (input == null) throw new ArtifactOperationException("The uploaded file is not a supported image.");
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new ArtifactOperationException("The uploaded file is not a supported image.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    actualFormat = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    decoded = reader.read(0);
                } finally { reader.dispose(); }
            }
            if (decoded == null || (long) decoded.getWidth() * decoded.getHeight() > MAX_PIXELS) {
                throw new ArtifactOperationException("The uploaded file is not a supported image.");
            }
            String suppliedType = upload.getContentType();
            boolean png = "png".equals(actualFormat) && MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(suppliedType);
            boolean jpeg = ("jpeg".equals(actualFormat) || "jpg".equals(actualFormat))
                    && (MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(suppliedType) || "image/jpg".equalsIgnoreCase(suppliedType));
            if (!png && !jpeg) throw new ArtifactOperationException("Only JPEG and PNG images are accepted.");
            String extension = png ? "png" : "jpg";
            String type = png ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
            BufferedImage normalized = decoded;
            if (jpeg && decoded.getType() != BufferedImage.TYPE_INT_RGB) {
                normalized = new BufferedImage(decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = normalized.createGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, decoded.getWidth(), decoded.getHeight());
                graphics.drawImage(decoded, 0, 0, null);
                graphics.dispose();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(normalized, extension, output)) throw new ArtifactOperationException("The image format is unsupported.");
            return new PreparedImage(output.toByteArray(), extension, type, safeDisplayName(upload.getOriginalFilename()));
        } catch (IOException exception) {
            throw new ArtifactOperationException("The uploaded image could not be read.");
        }
    }

    private String safeDisplayName(String name) {
        String normalized = name == null ? "image" : name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (normalized.isBlank()) normalized = "image";
        return normalized.length() > 255 ? normalized.substring(normalized.length() - 255) : normalized;
    }

    private void cleanupIfTransactionFails(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storage.delete(key);
            }
        });
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    private record PreparedImage(byte[] bytes, String extension, String contentType, String originalFilename) { }
    public record ImageContent(Resource resource, String contentType) { }
}
