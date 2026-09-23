package com.artifactalley.artifact;

import com.artifactalley.bid.BidRepository;
import com.artifactalley.bid.Bid;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArtifactManagementServiceTest {
    private final ArtifactRepository artifacts = mock(ArtifactRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BidRepository bids = mock(BidRepository.class);
    private final ArtifactImageService images = mock(ArtifactImageService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-23T08:00:00Z"), ZoneOffset.UTC);
    private final SellerArtifactService sellers = new SellerArtifactService(artifacts, users, bids, images, clock);
    private final ArtifactReviewService reviews = new ArtifactReviewService(artifacts, users, clock);
    private User seller;
    private User otherSeller;
    private User admin;

    @BeforeEach
    void setUp() {
        seller = user(1L, "Seller", "seller@example.com", Role.SELLER);
        otherSeller = user(2L, "Other", "other@example.com", Role.SELLER);
        admin = user(3L, "Admin", "admin@example.com", Role.ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(seller));
        when(users.findById(2L)).thenReturn(Optional.of(otherSeller));
        when(users.findById(3L)).thenReturn(Optional.of(admin));
    }

    @Test
    void dashboardUsesPersistedSellerIdQuery() {
        when(artifacts.findAllOwnedBy(1L)).thenReturn(List.of(pending(seller)));
        assertEquals(1, sellers.findOwned(1L).size());
        verify(artifacts).findAllOwnedBy(1L);
    }

    @Test
    void sellerCannotOpenAnotherSellersListing() {
        Artifact artifact = pending(otherSeller);
        when(artifacts.findByIdWithPeople(10L)).thenReturn(Optional.of(artifact));
        assertThrows(ArtifactOperationException.class, () -> sellers.details(10L, 1L));
    }

    @Test
    void pendingListingCanBeEditedWithoutChangingOwnerOrStatus() {
        Artifact artifact = pending(seller);
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        ArtifactSubmissionForm form = form("Corrected title");
        sellers.edit(10L, 1L, form);
        assertEquals("Corrected title", artifact.getTitle());
        assertEquals(ArtifactStatus.PENDING_APPROVAL, artifact.getStatus());
        assertSame(seller, artifact.getSeller());
    }

    @Test
    void rejectedListingCanBeEditedThenExplicitlyResubmitted() {
        Artifact artifact = pending(seller);
        artifact.reject(admin, "More provenance", LocalDateTime.now(clock));
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        sellers.edit(10L, 1L, form("Corrected"));
        assertEquals(ArtifactStatus.REJECTED, artifact.getStatus());
        sellers.resubmit(10L, 1L);
        assertEquals(ArtifactStatus.PENDING_APPROVAL, artifact.getStatus());
        assertNull(artifact.getRejectionReason());
    }

    @Test
    void liveListingCannotBeEditedOrResubmitted() {
        Artifact artifact = pending(seller);
        artifact.approve(admin, LocalDateTime.now(clock));
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        assertThrows(IllegalStateException.class, () -> sellers.edit(10L, 1L, form("Changed")));
        assertThrows(IllegalStateException.class, () -> sellers.resubmit(10L, 1L));
    }

    @Test
    void soldAndClosedListingsAreReadOnlyAndCannotBeWithdrawn() {
        User bidder = user(4L, "Bidder", "bidder@example.com", Role.BIDDER);
        Artifact sold = pending(seller); sold.approve(admin, LocalDateTime.now(clock));
        sold.settleSold(new Bid(sold, bidder, new BigDecimal("300.00"), LocalDateTime.now(clock)), LocalDateTime.now(clock));
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(sold));
        assertThrows(IllegalStateException.class, () -> sellers.edit(10L, 1L, form("Changed")));
        assertThrows(ArtifactOperationException.class, () -> sellers.withdraw(10L, 1L));

        Artifact closed = pending(seller); closed.approve(admin, LocalDateTime.now(clock));
        closed.settleClosed(LocalDateTime.now(clock));
        when(artifacts.findByIdForUpdate(11L)).thenReturn(Optional.of(closed));
        assertThrows(IllegalStateException.class, () -> sellers.edit(11L, 1L, form("Changed")));
        assertThrows(ArtifactOperationException.class, () -> sellers.withdraw(11L, 1L));
    }

    @Test
    void pendingAndRejectedListingsCanBeWithdrawn() {
        Artifact pending = pending(seller);
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(pending));
        sellers.withdraw(10L, 1L);
        assertEquals(ArtifactStatus.WITHDRAWN, pending.getStatus());

        Artifact rejected = pending(seller);
        rejected.reject(admin, "Changes", LocalDateTime.now(clock));
        when(artifacts.findByIdForUpdate(11L)).thenReturn(Optional.of(rejected));
        sellers.withdraw(11L, 1L);
        assertEquals(ArtifactStatus.WITHDRAWN, rejected.getStatus());
    }

    @Test
    void openLiveListingWithoutBidsCanBeWithdrawn() {
        Artifact artifact = pending(seller); artifact.approve(admin, LocalDateTime.now(clock));
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        when(bids.countByArtifactId(10L)).thenReturn(0L);
        sellers.withdraw(10L, 1L);
        assertEquals(ArtifactStatus.WITHDRAWN, artifact.getStatus());
    }

    @Test
    void liveListingWithBidCannotBeWithdrawn() {
        Artifact artifact = pending(seller); artifact.approve(admin, LocalDateTime.now(clock));
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        when(bids.countByArtifactId(10L)).thenReturn(1L);
        assertThrows(ArtifactOperationException.class, () -> sellers.withdraw(10L, 1L));
        assertEquals(ArtifactStatus.LIVE, artifact.getStatus());
    }

    @Test
    void administratorApprovalRecordsReviewerAndClearsReason() {
        Artifact artifact = pending(seller);
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        reviews.approve(10L, 3L);
        assertEquals(ArtifactStatus.LIVE, artifact.getStatus());
        assertSame(admin, artifact.getReviewedBy());
        assertNotNull(artifact.getReviewedAt());
        assertNull(artifact.getRejectionReason());
    }

    @Test
    void administratorRejectionTrimsReasonAndDuplicateReviewFails() {
        Artifact artifact = pending(seller);
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        reviews.reject(10L, 3L, "  Add provenance  ");
        assertEquals(ArtifactStatus.REJECTED, artifact.getStatus());
        assertEquals("Add provenance", artifact.getRejectionReason());
        assertSame(admin, artifact.getReviewedBy());
        assertThrows(ArtifactOperationException.class, () -> reviews.approve(10L, 3L));
    }

    @Test
    void blankRejectionReasonIsRejectedWithoutStateChange() {
        Artifact artifact = pending(seller);
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        assertThrows(IllegalArgumentException.class, () -> reviews.reject(10L, 3L, "  "));
        assertEquals(ArtifactStatus.PENDING_APPROVAL, artifact.getStatus());
    }

    @Test
    void nonAdminCannotReview() {
        Artifact artifact = pending(seller);
        when(artifacts.findByIdForUpdate(10L)).thenReturn(Optional.of(artifact));
        assertThrows(ArtifactOperationException.class, () -> reviews.approve(10L, 1L));
        assertEquals(ArtifactStatus.PENDING_APPROVAL, artifact.getStatus());
    }

    private Artifact pending(User owner) {
        Artifact artifact = new Artifact("Vase", Category.OTHER, "1900", new BigDecimal("100.00"),
                LocalDateTime.now(clock).plusDays(2), "Description", owner, owner.getEmail(),
                ArtifactStatus.PENDING_APPROVAL, LocalDateTime.now(clock));
        ReflectionTestUtils.setField(artifact, "id", 10L);
        return artifact;
    }

    private ArtifactSubmissionForm form(String title) {
        ArtifactSubmissionForm form = new ArtifactSubmissionForm();
        form.setTitle(title); form.setCategory(Category.OTHER); form.setEra("1901");
        form.setStartingPrice(new BigDecimal("200.00")); form.setClosesAt(LocalDateTime.now(clock).plusDays(3));
        form.setDescription("Updated description"); return form;
    }

    private User user(Long id, String name, String email, Role role) {
        User user = new User(name, email, "hash", role); ReflectionTestUtils.setField(user, "id", id); return user;
    }
}
