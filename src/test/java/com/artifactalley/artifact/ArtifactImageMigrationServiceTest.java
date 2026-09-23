package com.artifactalley.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArtifactImageMigrationServiceTest {
    @TempDir Path directory;

    @Test
    void verifiesRemoteCopyBeforeChangingMetadataAndSkipsCompletedRows() throws Exception {
        String localKey = "00000000-0000-0000-0000-000000000001.jpg";
        Files.write(directory.resolve(localKey), new byte[]{1, 2, 3});
        ArtifactImage image = new ArtifactImage(mock(Artifact.class), localKey, "photo.jpg", "image/jpeg", 0, true,
                LocalDateTime.now());
        ArtifactImageRepository repository = mock(ArtifactImageRepository.class);
        CloudinaryArtifactImageStorage cloud = mock(CloudinaryArtifactImageStorage.class);
        when(repository.findById(1L)).thenReturn(Optional.of(image));
        when(cloud.store(any(), eq("jpg"), eq("image/jpeg")))
                .thenReturn(new ArtifactImageStorage.StoredImage("cloudinary:artifact-alley/00000000-0000-0000-0000-000000000002.jpg", "image/jpeg"));
        when(cloud.open(anyString())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        ArtifactImageMigrationService service = new ArtifactImageMigrationService(repository, cloud, directory.toString());

        assertTrue(service.migrate(1L));
        assertTrue(image.getStorageKey().startsWith("cloudinary:"));
        verify(cloud).open(image.getStorageKey());
        verify(repository).saveAndFlush(image);

        assertFalse(service.migrate(1L));
        verify(cloud, times(1)).store(any(), anyString(), anyString());
    }
}
