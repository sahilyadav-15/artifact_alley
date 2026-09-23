package com.artifactalley.artifact;

import com.artifactalley.bid.Bid;
import com.artifactalley.bid.BidRepository;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(ArtifactSearchIntegrationTest.FixedClockConfiguration.class)
@Transactional
class ArtifactSearchIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 23, 10, 0);
    @Autowired ArtifactSearchService search;
    @Autowired ArtifactRepository artifacts;
    @Autowired BidRepository bids;
    @Autowired UserRepository users;
    private User seller;
    private User bidder;
    private String marker;

    @BeforeEach
    void setUp() {
        marker = "find" + Long.toHexString(System.nanoTime());
        seller = users.save(new User("Search Seller", marker + "-seller@test.invalid", "hash", Role.SELLER));
        bidder = users.save(new User("Search Bidder", marker + "-bidder@test.invalid", "hash", Role.BIDDER));
    }

    @Test
    void searchesTitleDescriptionEraAndCaseInsensitively() {
        Artifact title = live("Rare " + marker + " Astrolabe", Category.OTHER, "Victorian", "unrelated", 100, 2, 1);
        Artifact description = live("Plain vessel", Category.OTHER, "Modern", "Contains " + marker + " provenance", 200, 3, 2);
        Artifact era = live("Old coin", Category.COIN, marker.toUpperCase(), "unrelated", 300, 4, 3);
        artifacts.flush();

        List<Long> ids = result(new ArtifactSearchRequest("  " + marker.toUpperCase() + "  ", null, null, null,
                null, null, null, null, null)).getItems().stream().map(ArtifactSearchItem::getId).toList();
        assertThat(ids).containsExactly(title.getId(), description.getId(), era.getId());
        assertThat(result(request("definitely-no-match")).getItems()).isEmpty();
    }

    @Test
    void treatsPercentAndUnderscoreAsLiteralSearchCharacters() {
        Artifact literal = live(marker + " 100%_verified", Category.OTHER, "1900", "literal symbols", 100, 2, 1);
        live(marker + " 100xxverified", Category.OTHER, "1900", "different symbols", 100, 3, 2);
        artifacts.flush();
        ArtifactSearchResult found = result(request("100%_verified"));
        assertThat(found.getItems()).extracting(ArtifactSearchItem::getId).containsExactly(literal.getId());
    }

    @Test
    void combinesCategoryEraPriceAndClosingWindowFilters() {
        Artifact match = live(marker + " match", Category.COIN, "Mughal 1700", marker, 750, 1, 1);
        live(marker + " expensive", Category.COIN, "Mughal 1700", marker, 1500, 1, 2);
        live(marker + " late", Category.COIN, "Mughal 1700", marker, 800, 5, 3);
        live(marker + " category", Category.PAINTING, "Mughal 1700", marker, 800, 1, 4);
        artifacts.flush();
        ArtifactSearchResult found = result(new ArtifactSearchRequest(marker, "coin", "mughal", "700", "900",
                "3d", "ending-soon", null, null));
        assertThat(found.getItems()).extracting(ArtifactSearchItem::getId).containsExactly(match.getId());
        assertThat(found.getTotalItems()).isOne();
    }

    @Test
    void eachClosingWindowUsesTheCentralClock() {
        Artifact hours = live(marker + " hours", Category.OTHER, "A", marker, 100, 0, 1, NOW.plusHours(23));
        Artifact days = live(marker + " days", Category.OTHER, "A", marker, 100, 0, 2, NOW.plusDays(2));
        Artifact week = live(marker + " week", Category.OTHER, "A", marker, 100, 0, 3, NOW.plusDays(6));
        artifacts.flush();
        assertThat(result(window("24h")).getItems()).extracting(ArtifactSearchItem::getId).containsExactly(hours.getId());
        assertThat(result(window("3d")).getItems()).extracting(ArtifactSearchItem::getId).containsExactly(hours.getId(), days.getId());
        assertThat(result(window("7d")).getItems()).extracting(ArtifactSearchItem::getId).containsExactly(hours.getId(), days.getId(), week.getId());
    }

    @Test
    void excludesExpiredAndEveryNonLiveStatus() {
        Artifact active = live(marker + " active", Category.OTHER, "A", marker, 100, 2, 1);
        live(marker + " expired", Category.OTHER, "A", marker, 100, 0, 2, NOW.minusMinutes(1));
        for (ArtifactStatus status : List.of(ArtifactStatus.PENDING_APPROVAL, ArtifactStatus.REJECTED,
                ArtifactStatus.WITHDRAWN, ArtifactStatus.SOLD, ArtifactStatus.CLOSED)) {
            artifacts.save(new Artifact(marker + " " + status, Category.OTHER, "A", new BigDecimal("100"),
                    NOW.plusDays(2), marker, seller, seller.getEmail(), status, NOW.minusDays(1)));
        }
        artifacts.flush();
        assertThat(result(request(marker)).getItems()).extracting(ArtifactSearchItem::getId).containsExactly(active.getId());
    }

    @Test
    void appliesAllSortOptionsWithStableIdTieBreaking() {
        Artifact first = live(marker + " first", Category.OTHER, "A", marker, 300, 3, 3);
        Artifact second = live(marker + " second", Category.OTHER, "A", marker, 100, 1, 2);
        Artifact third = live(marker + " third", Category.OTHER, "A", marker, 200, 2, 1);
        bids.save(new Bid(first, bidder, new BigDecimal("400"), NOW.minusHours(2)));
        bids.save(new Bid(first, bidder, new BigDecimal("500"), NOW.minusHours(1)));
        bids.save(new Bid(third, bidder, new BigDecimal("300"), NOW.minusHours(1)));
        artifacts.flush(); bids.flush();
        assertOrder("ending-soon", second, third, first);
        assertOrder("newest", third, second, first);
        assertOrder("price-asc", second, third, first);
        assertOrder("price-desc", first, third, second);
        assertOrder("most-bids", first, third, second);
    }

    @Test
    void paginatesInTheDatabaseAndKeepsFilterLinksEncoded() {
        for (int index = 0; index < 13; index++) {
            live(marker + " item " + index, Category.OTHER, "A&B", marker, 100 + index, 1, index);
        }
        artifacts.flush();
        ArtifactSearchResult first = result(new ArtifactSearchRequest(marker, null, "A&B", null, null, null,
                null, "1", "12"));
        ArtifactSearchResult second = result(new ArtifactSearchRequest(marker, null, "A&B", null, null, null,
                null, "2", "12"));
        assertThat(first.getItems()).hasSize(12);
        assertThat(second.getItems()).hasSize(1);
        assertThat(first.getTotalItems()).isEqualTo(13);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getNextUrl()).contains("page=2").contains("era=A%26B");
        assertThat(second.getPreviousUrl()).doesNotContain("page=").contains("era=A%26B");
    }

    private ArtifactSearchRequest request(String query) {
        return new ArtifactSearchRequest(query, null, null, null, null, null, null, null, null);
    }
    private ArtifactSearchRequest window(String value) {
        return new ArtifactSearchRequest(marker, null, null, null, null, value, null, null, null);
    }
    private ArtifactSearchResult result(ArtifactSearchRequest request) { return search.searchActiveArtifacts(request); }
    private void assertOrder(String sort, Artifact... expected) {
        assertThat(result(new ArtifactSearchRequest(marker, null, null, null, null, null, sort, null, null)).getItems())
                .extracting(ArtifactSearchItem::getId).containsExactly(java.util.Arrays.stream(expected).map(Artifact::getId).toArray(Long[]::new));
    }
    private Artifact live(String title, Category category, String era, String description, int price, int days, int submittedDays) {
        return live(title, category, era, description, price, days, submittedDays, NOW.plusDays(days));
    }
    private Artifact live(String title, Category category, String era, String description, int price, int days,
                          int submittedDays, LocalDateTime closesAt) {
        Artifact artifact = new Artifact(title, category, era, new BigDecimal(price), closesAt, description,
                seller, seller.getEmail(), ArtifactStatus.LIVE, NOW.minusDays(submittedDays));
        artifact.updateCurrentPrice(new BigDecimal(price));
        return artifacts.save(artifact);
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
