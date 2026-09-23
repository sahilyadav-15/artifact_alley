package com.artifactalley.artifact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class LocalArtifactImageStorage implements ArtifactImageStorage {
    private final Path root;

    public LocalArtifactImageStorage(@Value("${artifactalley.images.directory:./uploads/artifacts}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
        try { Files.createDirectories(root); }
        catch (IOException exception) { throw new UncheckedIOException("Could not initialize artifact image storage.", exception); }
    }

    @Override
    public StoredImage store(byte[] content, String extension, String contentType) {
        String key = UUID.randomUUID() + "." + extension;
        Path target = safePath(key);
        try { Files.write(target, content, StandardOpenOption.CREATE_NEW); }
        catch (IOException exception) { throw new UncheckedIOException("Could not store the artifact image.", exception); }
        return new StoredImage(key, contentType);
    }

    @Override
    public Resource open(String storageKey) {
        try {
            Resource resource = new UrlResource(safePath(storageKey).toUri());
            if (!resource.exists() || !resource.isReadable()) throw new ArtifactOperationException("Image file is unavailable.");
            return resource;
        } catch (IOException exception) {
            throw new ArtifactOperationException("Image file is unavailable.");
        }
    }

    @Override
    public void delete(String storageKey) {
        try { Files.deleteIfExists(safePath(storageKey)); }
        catch (IOException exception) { throw new UncheckedIOException("Could not delete the artifact image.", exception); }
    }

    @Override
    public String publicUrl(Long imageId) { return "/artifact-images/" + imageId; }

    private Path safePath(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.(jpg|png)")) {
            throw new ArtifactOperationException("Invalid image reference.");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new ArtifactOperationException("Invalid image reference.");
        return resolved;
    }
}
