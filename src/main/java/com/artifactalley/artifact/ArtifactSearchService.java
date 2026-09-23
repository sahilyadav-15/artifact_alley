package com.artifactalley.artifact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArtifactSearchService {
    private final ArtifactSearchJdbcRepository repository;
    private final Clock clock;
    private final BigDecimal minimumIncrement;

    public ArtifactSearchService(ArtifactSearchJdbcRepository repository, Clock clock,
                                 @Value("${artifactalley.auction.minimum-increment:100.00}") BigDecimal minimumIncrement) {
        this.repository = repository; this.clock = clock; this.minimumIncrement = minimumIncrement;
    }

    @Transactional(readOnly = true)
    public ArtifactSearchResult searchActiveArtifacts(ArtifactSearchRequest request) {
        ArtifactSearchCriteria criteria = ArtifactSearchCriteria.normalize(request);
        LocalDateTime now = LocalDateTime.now(clock);
        ArtifactSearchJdbcRepository.SearchPage page = repository.search(criteria, now);
        List<ArtifactSearchItem> items = page.rows().stream().map(row -> new ArtifactSearchItem(
                row.id(), row.title(), row.category(), row.era(), row.startingPrice(), row.currentPrice(),
                row.closesAt(), row.description(), row.bidCount(),
                row.coverImageId() == null ? null : "/artifact-images/" + row.coverImageId(),
                row.currentPrice().add(minimumIncrement), endsIn(now, row.closesAt()))).toList();
        int totalPages = page.total() == 0 ? 0 : (int) Math.ceil((double) page.total() / criteria.size());
        List<ArtifactSearchResult.ActiveFilter> filters = activeFilters(criteria);
        List<ArtifactSearchResult.PageLink> links = pageLinks(criteria, totalPages);
        String previous = criteria.page() > 1 && totalPages > 0 ? url(criteria, criteria.page() - 1, null) : null;
        String next = criteria.page() < totalPages ? url(criteria, criteria.page() + 1, null) : null;
        return new ArtifactSearchResult(items, criteria, page.total(), filters, links, previous, next);
    }

    private List<ArtifactSearchResult.ActiveFilter> activeFilters(ArtifactSearchCriteria criteria) {
        List<ArtifactSearchResult.ActiveFilter> values = new ArrayList<>();
        add(values, criteria, "q", "Search", criteria.query());
        add(values, criteria, "category", "Category", criteria.category() == null ? null : criteria.category().name());
        add(values, criteria, "era", "Era", criteria.era());
        add(values, criteria, "minPrice", "Minimum price",
                criteria.minimumPrice() == null ? null : "₹" + criteria.minimumPrice().toPlainString());
        add(values, criteria, "maxPrice", "Maximum price",
                criteria.maximumPrice() == null ? null : "₹" + criteria.maximumPrice().toPlainString());
        add(values, criteria, "endingWithin", "Closing",
                criteria.endingWithin() == null ? null : criteria.endingWithin().getLabel());
        return values;
    }

    private void add(List<ArtifactSearchResult.ActiveFilter> target, ArtifactSearchCriteria criteria,
                     String field, String label, String value) {
        if (value != null) target.add(new ArtifactSearchResult.ActiveFilter(field, label, value,
                url(criteria, 1, field)));
    }

    private List<ArtifactSearchResult.PageLink> pageLinks(ArtifactSearchCriteria criteria, int totalPages) {
        if (totalPages <= 1) return List.of();
        Set<Integer> numbers = new LinkedHashSet<>();
        numbers.add(1);
        for (int number = Math.max(1, criteria.page() - 2); number <= Math.min(totalPages, criteria.page() + 2); number++) {
            numbers.add(number);
        }
        numbers.add(totalPages);
        return numbers.stream().sorted().map(number -> new ArtifactSearchResult.PageLink(
                number, url(criteria, number, null), number == criteria.page())).toList();
    }

    private String url(ArtifactSearchCriteria criteria, int page, String omit) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
        param(builder, "q", criteria.query(), omit);
        param(builder, "category", criteria.category() == null ? null : criteria.category().name(), omit);
        param(builder, "era", criteria.era(), omit);
        param(builder, "minPrice", criteria.minimumPrice() == null ? null : criteria.minimumPrice().toPlainString(), omit);
        param(builder, "maxPrice", criteria.maximumPrice() == null ? null : criteria.maximumPrice().toPlainString(), omit);
        param(builder, "endingWithin", criteria.endingWithin() == null ? null : criteria.endingWithin().getValue(), omit);
        if (!"sort".equals(omit) && criteria.sort() != ArtifactSearchCriteria.SortOption.ENDING_SOON) {
            builder.queryParam("sort", criteria.sort().getValue());
        }
        if (criteria.size() != ArtifactSearchCriteria.DEFAULT_SIZE) builder.queryParam("size", criteria.size());
        if (page > 1) builder.queryParam("page", page);
        return builder.build().encode().toUriString();
    }

    private void param(UriComponentsBuilder builder, String name, Object value, String omit) {
        if (!name.equals(omit) && value != null) builder.queryParam(name, value);
    }

    private String endsIn(LocalDateTime now, LocalDateTime closesAt) {
        Duration duration = Duration.between(now, closesAt);
        long minutes = Math.max(1, duration.toMinutes());
        if (minutes < 60) return "Ends in " + minutes + (minutes == 1 ? " minute" : " minutes");
        long hours = duration.toHours();
        if (hours < 48) return "Ends in " + hours + (hours == 1 ? " hour" : " hours");
        long days = duration.toDays();
        return "Ends in " + days + (days == 1 ? " day" : " days");
    }
}
