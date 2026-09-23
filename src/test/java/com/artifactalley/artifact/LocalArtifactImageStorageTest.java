package com.artifactalley.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalArtifactImageStorageTest {
    @TempDir Path directory;

    @Test
    void generatedKeysStayInsideConfiguredDirectoryAndCanBeReadThenDeleted() throws Exception {
        LocalArtifactImageStorage storage = new LocalArtifactImageStorage(directory.toString());
        ArtifactImageStorage.StoredImage stored = storage.store(new byte[]{1, 2, 3}, "png", "image/png");
        assertTrue(stored.storageKey().matches("[0-9a-f-]{36}\\.png"));
        assertArrayEquals(new byte[]{1, 2, 3}, storage.open(stored.storageKey()).getInputStream().readAllBytes());
        storage.delete(stored.storageKey());
        assertEquals(0, Files.list(directory).count());
    }

    @Test
    void traversalAndAbsoluteKeysAreRejected() {
        LocalArtifactImageStorage storage = new LocalArtifactImageStorage(directory.toString());
        assertThrows(ArtifactOperationException.class, () -> storage.open("../outside.png"));
        assertThrows(ArtifactOperationException.class, () -> storage.open("/tmp/outside.png"));
    }
}
