package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactRepository;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.artifact.Category;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceTest {
    @Mock private ArtifactRepository artifactRepository;
    @Mock private UserRepository userRepository;
    @Mock private BidRepository bidRepository;
    private BiddingService biddingService;
    private Artifact liveArtifact;
    private User bidder;

    @BeforeEach
    void setUp() {
        biddingService = new BiddingService(artifactRepository, userRepository, bidRepository, new BigDecimal("100.00"));
        liveArtifact = artifact(ArtifactStatus.LIVE, LocalDateTime.now().plusHours(2), "seller@example.com");
        bidder = user(Role.BIDDER, "bidder@example.com");
        lenient().when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(liveArtifact));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(bidder));
        lenient().when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void successfulBidStoresAuthenticatedBidderTimestampAndUpdatesCurrentPrice() {
        LocalDateTime before = LocalDateTime.now();
        Bid bid = biddingService.placeBid(1L, 2L, new BigDecimal("1200.00"));

        assertSame(bidder, bid.getBidder());
        assertSame(liveArtifact, bid.getArtifact());
        assertEquals(new BigDecimal("1200.00"), bid.getAmount());
        assertFalse(bid.getPlacedAt().isBefore(before));
        assertFalse(bid.getPlacedAt().isAfter(LocalDateTime.now()));
        assertEquals(new BigDecimal("1200.00"), liveArtifact.getCurrentPrice());
        verify(bidRepository).save(bid);
        verify(artifactRepository).save(liveArtifact);
    }

    @Test
    void bidExactlyAtMinimumIsAccepted() {
        assertDoesNotThrow(() -> biddingService.placeBid(1L, 2L, new BigDecimal("1100.00")));
    }

    @Test
    void insufficientEqualZeroAndNegativeBidsAreRejectedWithoutWriting() {
        for (String amount : List.of("1099.99", "1000.00", "0.00", "-5.00")) {
            assertThrows(BidBelowMinimumException.class,
                    () -> biddingService.placeBid(1L, 2L, new BigDecimal(amount)));
        }
        assertEquals(new BigDecimal("1000.00"), liveArtifact.getCurrentPrice());
        verify(bidRepository, never()).save(any());
        verify(artifactRepository, never()).save(any());
    }

    @Test
    void closedAuctionIsRejected() {
        liveArtifact = artifact(ArtifactStatus.LIVE, LocalDateTime.now().minusSeconds(1), "seller@example.com");
        when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(liveArtifact));
        BiddingException exception = assertThrows(BidNotAllowedException.class,
                () -> biddingService.placeBid(1L, 2L, new BigDecimal("1100.00")));
        assertTrue(exception.getMessage().contains("closed"));
        verifyNoWrites();
    }

    @Test
    void everyNonLiveStatusIsRejected() {
        for (ArtifactStatus status : List.of(ArtifactStatus.PENDING_APPROVAL, ArtifactStatus.SOLD, ArtifactStatus.CLOSED)) {
            liveArtifact = artifact(status, LocalDateTime.now().plusHours(2), "seller@example.com");
            when(artifactRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(liveArtifact));
            assertThrows(BidNotAllowedException.class,
                    () -> biddingService.placeBid(1L, 2L, new BigDecimal("1100.00")));
        }
        verifyNoWrites();
    }

    @Test
    void sellerAndAdministratorRolesAreRejected() {
        for (Role role : List.of(Role.SELLER, Role.ADMIN)) {
            when(userRepository.findById(2L)).thenReturn(Optional.of(user(role, role + "@example.com")));
            assertThrows(BidNotAllowedException.class,
                    () -> biddingService.placeBid(1L, 2L, new BigDecimal("1100.00")));
        }
        verifyNoWrites();
    }

    @Test
    void submittingSellerCannotBidOnOwnListing() {
        bidder = user(Role.BIDDER, "SELLER@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(bidder));
        assertThrows(BidNotAllowedException.class,
                () -> biddingService.placeBid(1L, 2L, new BigDecimal("1100.00")));
        verifyNoWrites();
    }

    @Test
    void missingArtifactAndUserAreReported() {
        when(artifactRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(ArtifactNotFoundException.class,
                () -> biddingService.placeBid(99L, 2L, new BigDecimal("1100.00")));

        when(userRepository.findById(88L)).thenReturn(Optional.empty());
        assertThrows(BidderNotFoundException.class,
                () -> biddingService.placeBid(1L, 88L, new BigDecimal("1100.00")));
        verifyNoWrites();
    }

    @Test
    void bidHistoryUsesRepositoryNewestFirstOrderAndMasksNames() {
        User first = user(Role.BIDDER, "first@example.com");
        User second = user(Role.BIDDER, "second@example.com");
        LocalDateTime newest = LocalDateTime.now();
        when(artifactRepository.findById(1L)).thenReturn(Optional.of(liveArtifact));
        when(bidRepository.findByArtifactIdOrderByPlacedAtDescIdDesc(1L)).thenReturn(List.of(
                new Bid(liveArtifact, first, new BigDecimal("1300.00"), newest),
                new Bid(liveArtifact, second, new BigDecimal("1200.00"), newest.minusMinutes(2))));

        AuctionDetails details = biddingService.getAuctionDetails(1L);

        assertEquals(List.of(new BigDecimal("1300.00"), new BigDecimal("1200.00")),
                details.getBidHistory().stream().map(BidHistoryItem::getAmount).toList());
        assertEquals(List.of("B***", "B***"),
                details.getBidHistory().stream().map(BidHistoryItem::getBidderDisplayName).toList());
    }

    private Artifact artifact(ArtifactStatus status, LocalDateTime closesAt, String sellerEmail) {
        return new Artifact("Test Artifact", Category.OTHER, "1900", new BigDecimal("1000.00"), closesAt,
                "Description", sellerEmail, status);
    }

    private User user(Role role, String email) {
        return new User("Bidder Name", email, "hash", role);
    }

    private void verifyNoWrites() {
        verify(bidRepository, never()).save(any());
        verify(artifactRepository, never()).save(any());
    }
}
