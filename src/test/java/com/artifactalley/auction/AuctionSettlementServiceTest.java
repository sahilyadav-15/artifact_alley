package com.artifactalley.auction;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.artifact.Category;
import com.artifactalley.bid.Bid;
import com.artifactalley.bid.BidRepository;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSettlementServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-23T08:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    @Mock private ArtifactRepository artifactRepository;
    @Mock private BidRepository bidRepository;
    private AuctionSettlementService service;

    @BeforeEach
    void setUp() {
        service = new AuctionSettlementService(artifactRepository, bidRepository, CLOCK);
    }

    @Test
    void expiredAuctionWithHighestBidBecomesSoldAndRecordsOutcome() {
        Artifact artifact = artifact(ArtifactStatus.LIVE, NOW.minusMinutes(1));
        Bid winner = new Bid(artifact, bidder("winner@example.com"), new BigDecimal("1400.00"), NOW.minusMinutes(2));
        when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(artifact));
        when(bidRepository.findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(1L)).thenReturn(Optional.of(winner));
        when(artifactRepository.save(artifact)).thenReturn(artifact);

        Artifact result = service.settleAuction(1L);

        assertEquals(ArtifactStatus.SOLD, result.getStatus());
        assertSame(winner, result.getWinningBid());
        assertSame(winner.getBidder(), result.getWinningBid().getBidder());
        assertEquals(new BigDecimal("1400.00"), result.getCurrentPrice());
        assertEquals(NOW, result.getSettledAt());
    }

    @Test
    void expiredAuctionWithoutBidsClosesWithoutWinner() {
        Artifact artifact = artifact(ArtifactStatus.LIVE, NOW.minusSeconds(1));
        when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(artifact));
        when(bidRepository.findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(1L)).thenReturn(Optional.empty());
        when(artifactRepository.save(artifact)).thenReturn(artifact);

        service.settleAuction(1L);

        assertEquals(ArtifactStatus.CLOSED, artifact.getStatus());
        assertNull(artifact.getWinningBid());
        assertEquals(new BigDecimal("1000.00"), artifact.getCurrentPrice());
        assertEquals(NOW, artifact.getSettledAt());
    }

    @Test
    void futureAuctionIsNotSettled() {
        Artifact artifact = artifact(ArtifactStatus.LIVE, NOW.plusSeconds(1));
        when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(artifact));
        assertThrows(AuctionNotExpiredException.class, () -> service.settleAuction(1L));
        assertEquals(ArtifactStatus.LIVE, artifact.getStatus());
        verify(bidRepository, never()).findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(anyLong());
        verify(artifactRepository, never()).save(any());
    }

    @Test
    void pendingSoldAndClosedAuctionsRemainUnchanged() {
        Artifact pending = artifact(ArtifactStatus.PENDING_APPROVAL, NOW.minusDays(1));
        Artifact sold = artifact(ArtifactStatus.LIVE, NOW.minusDays(1));
        sold.settleSold(new Bid(sold, bidder("winner@example.com"), new BigDecimal("1100.00"), NOW.minusDays(1)), NOW.minusHours(2));
        Artifact closed = artifact(ArtifactStatus.LIVE, NOW.minusDays(1));
        closed.settleClosed(NOW.minusHours(2));

        for (Artifact artifact : new Artifact[]{pending, sold, closed}) {
            when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(artifact));
            ArtifactStatus before = artifact.getStatus();
            LocalDateTime settledAt = artifact.getSettledAt();
            assertSame(artifact, service.settleAuction(1L));
            assertEquals(before, artifact.getStatus());
            assertEquals(settledAt, artifact.getSettledAt());
        }
        verifyNoInteractions(bidRepository);
        verify(artifactRepository, never()).save(any());
    }

    @Test
    void repeatedSettlementIsIdempotentAndWinnerStaysStable() {
        Artifact artifact = artifact(ArtifactStatus.LIVE, NOW.minusMinutes(1));
        Bid winner = new Bid(artifact, bidder("winner@example.com"), new BigDecimal("1200.00"), NOW.minusMinutes(2));
        when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(artifact));
        when(bidRepository.findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(1L)).thenReturn(Optional.of(winner));
        when(artifactRepository.save(artifact)).thenReturn(artifact);

        service.settleAuction(1L);
        LocalDateTime firstSettlement = artifact.getSettledAt();
        service.settleAuction(1L);

        assertSame(winner, artifact.getWinningBid());
        assertEquals(firstSettlement, artifact.getSettledAt());
        verify(bidRepository, times(1)).findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(1L);
        verify(artifactRepository, times(1)).save(artifact);
    }

    @Test
    void inconsistentWinningBidFailsBeforeArtifactChanges() {
        Artifact artifact = artifact(ArtifactStatus.LIVE, NOW.minusMinutes(1));
        Artifact other = artifact(ArtifactStatus.LIVE, NOW.minusMinutes(1));
        Bid invalidWinner = new Bid(other, bidder("other@example.com"), new BigDecimal("5000.00"), NOW.minusMinutes(2));
        when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(artifact));
        when(bidRepository.findFirstByArtifactIdOrderByAmountDescPlacedAtAscIdAsc(1L)).thenReturn(Optional.of(invalidWinner));

        assertThrows(IllegalArgumentException.class, () -> service.settleAuction(1L));
        assertEquals(ArtifactStatus.LIVE, artifact.getStatus());
        assertNull(artifact.getWinningBid());
        assertNull(artifact.getSettledAt());
        verify(artifactRepository, never()).save(any());
    }

    private Artifact artifact(ArtifactStatus status, LocalDateTime closesAt) {
        return new Artifact("Lot", Category.OTHER, "1900", new BigDecimal("1000.00"), closesAt,
                "Description", "seller@example.com", status);
    }

    private User bidder(String email) {
        return new User("Bidder", email, "hash", Role.BIDDER);
    }
}
