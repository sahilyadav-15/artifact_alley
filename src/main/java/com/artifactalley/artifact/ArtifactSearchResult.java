package com.artifactalley.artifact;

import java.util.List;
import java.util.Map;

public final class ArtifactSearchResult {
    private final List<ArtifactSearchItem> items;
    private final ArtifactSearchCriteria criteria;
    private final long totalItems;
    private final int totalPages;
    private final List<ActiveFilter> activeFilters;
    private final List<PageLink> pageLinks;
    private final String previousUrl;
    private final String nextUrl;

    public ArtifactSearchResult(List<ArtifactSearchItem> items, ArtifactSearchCriteria criteria, long totalItems,
                                List<ActiveFilter> activeFilters, List<PageLink> pageLinks,
                                String previousUrl, String nextUrl) {
        this.items = List.copyOf(items); this.criteria = criteria; this.totalItems = totalItems;
        this.totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / criteria.size());
        this.activeFilters = List.copyOf(activeFilters); this.pageLinks = List.copyOf(pageLinks);
        this.previousUrl = previousUrl; this.nextUrl = nextUrl;
    }

    public List<ArtifactSearchItem> getItems() { return items; }
    public ArtifactSearchCriteria getCriteria() { return criteria; }
    public long getTotalItems() { return totalItems; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return criteria.page(); }
    public int getPageSize() { return criteria.size(); }
    public List<ActiveFilter> getActiveFilters() { return activeFilters; }
    public boolean isFiltered() { return !activeFilters.isEmpty(); }
    public List<PageLink> getPageLinks() { return pageLinks; }
    public String getPreviousUrl() { return previousUrl; }
    public String getNextUrl() { return nextUrl; }
    public String getSort() { return criteria.sort().getValue(); }
    public Map<String, String> getAppliedFilters() {
        return activeFilters.stream().collect(java.util.stream.Collectors.toMap(
                ActiveFilter::field, ActiveFilter::value, (left, right) -> left, java.util.LinkedHashMap::new));
    }

    public static final class ActiveFilter {
        private final String field;
        private final String label;
        private final String value;
        private final String removeUrl;
        public ActiveFilter(String field, String label, String value, String removeUrl) {
            this.field = field; this.label = label; this.value = value; this.removeUrl = removeUrl;
        }
        public String field() { return field; }
        public String value() { return value; }
        public String getField() { return field; }
        public String getLabel() { return label; }
        public String getValue() { return value; }
        public String getRemoveUrl() { return removeUrl; }
    }
    public static final class PageLink {
        private final int number;
        private final String url;
        private final boolean current;
        public PageLink(int number, String url, boolean current) {
            this.number = number; this.url = url; this.current = current;
        }
        public int getNumber() { return number; }
        public String getUrl() { return url; }
        public boolean isCurrent() { return current; }
    }
}
