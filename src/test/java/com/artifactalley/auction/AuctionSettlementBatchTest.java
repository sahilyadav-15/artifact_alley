package com.artifactalley.auction;

import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.ArtifactStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSettlementBatchTest {
    @Mock private ArtifactRepository artifactRepository;
    @Mock private AuctionSettlementService settlementService;

    @Test
    void boundedSelectionContinuesAfterOneSettlementFailure() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-23T08:00:00Z"), ZoneOffset.UTC);
        when(artifactRepository.findExpiredIds(eq(ArtifactStatus.LIVE), eq(LocalDateTime.now(clock)), any(Pageable.class)))
                .thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("malformed row")).when(settlementService).settleAuction(1L);
        AuctionSettlementBatch batch = new AuctionSettlementBatch(artifactRepository, settlementService, clock, 2);

        assertEquals(1, batch.settleExpiredBatch());

        verify(settlementService).settleAuction(1L);
        verify(settlementService).settleAuction(2L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(artifactRepository).findExpiredIds(eq(ArtifactStatus.LIVE), eq(LocalDateTime.now(clock)), pageable.capture());
        assertEquals(2, pageable.getValue().getPageSize());
    }
}
