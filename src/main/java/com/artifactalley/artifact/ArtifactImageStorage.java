package com.artifactalley.artifact;

import org.springframework.core.io.Resource;

public interface ArtifactImageStorage {
    record StoredImage(String storageKey, String contentType) { }
    StoredImage store(byte[] content, String extension, String contentType);
    Resource open(String storageKey);
    void delete(String storageKey);
    String publicUrl(Long imageId);
}
