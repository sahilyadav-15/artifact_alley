package com.artifactalley.artifact;

import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArtifactImageServiceTest {
    private final ArtifactRepository artifacts = mock(ArtifactRepository.class);
    private final ArtifactImageRepository images = mock(ArtifactImageRepository.class);
    private final ArtifactImageStorage storage = mock(ArtifactImageStorage.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-23T08:00:00Z"), ZoneOffset.UTC);
    private final ArtifactImageService service = new ArtifactImageService(artifacts, images, storage, clock);
    private Artifact artifact;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        User seller = new User("Seller", "seller@example.com", "hash", Role.SELLER);
        ReflectionTestUtils.setField(seller, "id", 1L);
        artifact = new Artifact("Vase", Category.OTHER, "1900", new BigDecimal("100"),
                LocalDateTime.now(clock).plusDays(1), "Description", seller, seller.getEmail(),
                ArtifactStatus.PENDING_APPROVAL, LocalDateTime.now(clock));
        ReflectionTestUtils.setField(artifact, "id", 10L);
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        when(storage.store(any(), anyString(), anyString())).thenReturn(new ArtifactImageStorage.StoredImage("00000000-0000-0000-0000-000000000001.png", "image/png"));
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void validPngIsDecodedReencodedAndSavedAsCover() throws Exception {
        when(images.countByArtifactId(10L)).thenReturn(0L);
        service.upload(10L, 1L, List.of(file("photo.png", "image/png", image("png"))));
        ArgumentCaptor<ArtifactImage> capture = ArgumentCaptor.forClass(ArtifactImage.class);
        verify(images).save(capture.capture());
        assertTrue(capture.getValue().isCoverImage());
        assertEquals("image/png", capture.getValue().getContentType());
        verify(storage).store(any(), eq("png"), eq("image/png"));
    }

    @Test
    void validJpegIsAcceptedAndNormalized() throws Exception {
        when(images.countByArtifactId(10L)).thenReturn(0L);
        when(storage.store(any(), anyString(), anyString())).thenReturn(new ArtifactImageStorage.StoredImage("00000000-0000-0000-0000-000000000001.jpg", "image/jpeg"));
        service.upload(10L, 1L, List.of(file("photo.jpeg", "image/jpeg", image("jpg"))));
        verify(storage).store(any(), eq("jpg"), eq("image/jpeg"));
    }

    @Test
    void emptyOversizedFakeAndUnsupportedFilesAreRejected() throws Exception {
        assertThrows(ArtifactOperationException.class, () -> service.upload(10L, 1L,
                List.of(file("empty.png", "image/png", new byte[0]))));
        assertThrows(ArtifactOperationException.class, () -> service.upload(10L, 1L,
                List.of(file("large.png", "image/png", new byte[(int) ArtifactImageService.MAX_BYTES + 1]))));
        assertThrows(ArtifactOperationException.class, () -> service.upload(10L, 1L,
                List.of(file("fake.png", "image/png", "not an image".getBytes()))));
        assertThrows(ArtifactOperationException.class, () -> service.upload(10L, 1L,
                List.of(file("photo.gif", "image/gif", image("png")))));
        assertThrows(ArtifactOperationException.class, () -> service.upload(10L, 1L,
                List.of(file("disguised.png", "image/png", image("gif")))));
        verify(storage, never()).store(any(), anyString(), anyString());
    }

    @Test
    void maximumFiveImagesIsEnforced() throws Exception {
        when(images.countByArtifactId(10L)).thenReturn(5L);
        assertThrows(ArtifactOperationException.class, () -> service.upload(10L, 1L,
                List.of(file("sixth.png", "image/png", image("png")))));
        verify(storage, never()).store(any(), anyString(), anyString());
    }

    @Test
    void traversalFilenameIsDisplayOnlyAndCannotBecomeStorageKey() throws Exception {
        when(images.countByArtifactId(10L)).thenReturn(0L);
        service.upload(10L, 1L, List.of(file("../../outside.png", "image/png", image("png"))));
        ArgumentCaptor<ArtifactImage> capture = ArgumentCaptor.forClass(ArtifactImage.class);
        verify(images).save(capture.capture());
        assertEquals("outside.png", capture.getValue().getOriginalFilename());
        assertFalse(capture.getValue().getStorageKey().contains("outside"));
    }

    @Test
    void nonOwnerCannotUpload() throws Exception {
        assertThrows(ArtifactOperationException.class, () -> service.upload(10L, 99L,
                List.of(file("photo.png", "image/png", image("png")))));
        verify(storage, never()).store(any(), anyString(), anyString());
    }

    @Test
    void databaseFailureCleansStoredFileOnRollback() throws Exception {
        when(images.countByArtifactId(10L)).thenReturn(0L);
        doThrow(new RuntimeException("database failed")).when(images).flush();
        assertThrows(RuntimeException.class, () -> service.upload(10L, 1L,
                List.of(file("photo.png", "image/png", image("png")))));
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(storage).delete("00000000-0000-0000-0000-000000000001.png");
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
    }

    @Test
    void deletingCoverSelectsFirstRemainingImageAndDeletesFileAfterCommit() {
        ArtifactImage cover = image(1L, "00000000-0000-0000-0000-000000000001.png", 0, true);
        ArtifactImage remaining = image(2L, "00000000-0000-0000-0000-000000000002.png", 1, false);
        when(images.findById(1L)).thenReturn(Optional.of(cover));
        when(images.findByArtifactIdOrderByDisplayOrderAscIdAsc(10L)).thenReturn(List.of(remaining));
        service.delete(10L, 1L, 1L);
        assertTrue(remaining.isCoverImage());
        assertEquals(0, remaining.getDisplayOrder());
        verify(storage, never()).delete(anyString());
        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(storage).delete(cover.getStorageKey());
    }

    @Test
    void coverSelectionAndReorderingRequireExactOwnedImageSet() {
        ArtifactImage first = image(1L, "00000000-0000-0000-0000-000000000001.png", 0, true);
        ArtifactImage second = image(2L, "00000000-0000-0000-0000-000000000002.png", 1, false);
        when(images.findByArtifactIdOrderByDisplayOrderAscIdAsc(10L)).thenReturn(List.of(first, second));
        service.selectCover(10L, 2L, 1L);
        assertFalse(first.isCoverImage()); assertTrue(second.isCoverImage());
        service.reorder(10L, 1L, List.of(2L, 1L));
        assertEquals(1, first.getDisplayOrder()); assertEquals(0, second.getDisplayOrder());
        assertThrows(ArtifactOperationException.class, () -> service.reorder(10L, 1L, List.of(2L, 99L)));
    }

    private MockMultipartFile file(String name, String type, byte[] data) { return new MockMultipartFile("images", name, type, data); }
    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB()); ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output); return output.toByteArray();
    }
    private ArtifactImage image(Long id, String key, int order, boolean cover) {
        ArtifactImage image = new ArtifactImage(artifact, key, "photo.png", "image/png", order, cover, LocalDateTime.now(clock));
        ReflectionTestUtils.setField(image, "id", id); return image;
    }
}
