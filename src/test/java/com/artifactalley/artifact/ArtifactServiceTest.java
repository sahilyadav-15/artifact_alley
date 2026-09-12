package com.artifactalley.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceTest {
    @Mock private ArtifactRepository artifactRepository;
    @InjectMocks private ArtifactService artifactService;

    @Test
    void sellerSubmissionIsPendingAndDoesNotBecomeLiveUntilApproved() {
        when(artifactRepository.save(any(Artifact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Artifact submitted = artifactService.submit("Bidriware Vase", Category.OTHER, "19th century",
                new BigDecimal("1500.00"), LocalDateTime.now().plusDays(2), "A silver-inlaid metal vase.", "seller@example.com");

        assertEquals(ArtifactStatus.PENDING_APPROVAL, submitted.getStatus());
        assertEquals("seller@example.com", submitted.getSubmittedByEmail());
        when(artifactRepository.findById(42L)).thenReturn(Optional.of(submitted));
        assertTrue(artifactService.approve(42L));
        assertEquals(ArtifactStatus.LIVE, submitted.getStatus());
        verify(artifactRepository, times(2)).save(submitted);
    }

    @Test
    void cannotApproveAnArtifactThatIsNotPending() {
        Artifact live = new Artifact("Live Coin", Category.COIN, "1911", new BigDecimal("1000.00"),
                LocalDateTime.now().plusDays(2), "Already published.");
        when(artifactRepository.findById(1L)).thenReturn(Optional.of(live));

        assertFalse(artifactService.approve(1L));
        verify(artifactRepository, never()).save(any());
    }
}
