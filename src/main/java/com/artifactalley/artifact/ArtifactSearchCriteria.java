package com.artifactalley.artifact;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/** Fully normalized and validated database-search criteria. Page numbers are one-based. */
public record ArtifactSearchCriteria(String query, Category category, String era,
                                     BigDecimal minimumPrice, BigDecimal maximumPrice,
                                     ClosingWindow endingWithin, SortOption sort,
                                     int page, int size) {
    public static final int DEFAULT_SIZE = 12;
    public static final int MAX_SIZE = 48;
    public static final int MAX_QUERY_LENGTH = 100;
    private static final Set<Integer> SIZES = Set.of(12, 24, 48);

    public enum ClosingWindow {
        HOURS_24("24h", "Ending in 24 hours", 1),
        DAYS_3("3d", "Ending in 3 days", 3),
        DAYS_7("7d", "Ending in 7 days", 7);

        private final String value;
        private final String label;
        private final int days;
        ClosingWindow(String value, String label, int days) {
            this.value = value; this.label = label; this.days = days;
        }
        public String getValue() { return value; }
        public String getLabel() { return label; }
        public int getDays() { return days; }
    }

    public enum SortOption {
        ENDING_SOON("ending-soon", "Ending soon"),
        NEWEST("newest", "Newly listed"),
        PRICE_ASC("price-asc", "Price: low to high"),
        PRICE_DESC("price-desc", "Price: high to low"),
        MOST_BIDS("most-bids", "Most bids");

        private final String value;
        private final String label;
        SortOption(String value, String label) { this.value = value; this.label = label; }
        public String getValue() { return value; }
        public String getLabel() { return label; }
    }

    public static ArtifactSearchCriteria normalize(ArtifactSearchRequest request) {
        String query = trimToNull(request == null ? null : request.query());
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new ArtifactSearchValidationException("q", "Search text must be at most 100 characters.");
        }
        String era = trimToNull(request == null ? null : request.era());
        if (era != null && era.length() > 60) {
            throw new ArtifactSearchValidationException("era", "Era must be at most 60 characters.");
        }
        Category category = parseCategory(request == null ? null : request.category());
        BigDecimal minimum = parsePrice(request == null ? null : request.minimumPrice(), "minPrice", "Minimum price");
        BigDecimal maximum = parsePrice(request == null ? null : request.maximumPrice(), "maxPrice", "Maximum price");
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new ArtifactSearchValidationException("minPrice", "Minimum price cannot exceed maximum price.");
        }
        ClosingWindow window = parseWindow(request == null ? null : request.endingWithin());
        SortOption sort = parseSort(request == null ? null : request.sort());
        int page = parsePositive(request == null ? null : request.page(), "page", 1, false);
        int size = parsePositive(request == null ? null : request.size(), "size", DEFAULT_SIZE, true);
        if (!SIZES.contains(size)) {
            throw new ArtifactSearchValidationException("size", "Page size must be 12, 24 or 48.");
        }
        return new ArtifactSearchCriteria(query, category, era, minimum, maximum, window, sort, page, size);
    }

    private static Category parseCategory(String raw) {
        String value = trimToNull(raw);
        if (value == null) return null;
        try { return Category.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) {
            throw new ArtifactSearchValidationException("category", "Choose a valid artifact category.");
        }
    }

    private static ClosingWindow parseWindow(String raw) {
        String value = trimToNull(raw);
        if (value == null) return null;
        for (ClosingWindow option : ClosingWindow.values()) if (option.value.equals(value)) return option;
        throw new ArtifactSearchValidationException("endingWithin", "Choose a valid closing-time window.");
    }

    private static SortOption parseSort(String raw) {
        String value = trimToNull(raw);
        if (value == null) return SortOption.ENDING_SOON;
        for (SortOption option : SortOption.values()) if (option.value.equals(value)) return option;
        throw new ArtifactSearchValidationException("sort", "Choose a supported sort option.");
    }

    private static BigDecimal parsePrice(String raw, String field, String label) {
        String value = trimToNull(raw);
        if (value == null) return null;
        try {
            BigDecimal price = new BigDecimal(value);
            if (price.signum() < 0) throw new NumberFormatException();
            if (price.scale() > 2 || price.precision() - price.scale() > 10) throw new NumberFormatException();
            return price;
        } catch (NumberFormatException exception) {
            throw new ArtifactSearchValidationException(field, label + " must be a nonnegative amount with at most two decimal places.");
        }
    }

    private static int parsePositive(String raw, String field, int fallback, boolean rejectNonPositive) {
        String value = trimToNull(raw);
        if (value == null) return fallback;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                if (!rejectNonPositive && field.equals("page")) return 1;
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ArtifactSearchValidationException(field,
                    field.equals("page") ? "Page must be a positive whole number." : "Choose a supported page size.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
