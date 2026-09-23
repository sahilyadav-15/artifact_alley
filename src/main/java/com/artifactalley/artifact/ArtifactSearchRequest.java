package com.artifactalley.artifact;

/** Raw, immutable values shared by the JSP and REST adapters before normalization. */
public final class ArtifactSearchRequest {
    private final String query;
    private final String category;
    private final String era;
    private final String minimumPrice;
    private final String maximumPrice;
    private final String endingWithin;
    private final String sort;
    private final String page;
    private final String size;

    public ArtifactSearchRequest(String query, String category, String era, String minimumPrice,
                                 String maximumPrice, String endingWithin, String sort, String page, String size) {
        this.query = query; this.category = category; this.era = era; this.minimumPrice = minimumPrice;
        this.maximumPrice = maximumPrice; this.endingWithin = endingWithin; this.sort = sort;
        this.page = page; this.size = size;
    }

    public String query() { return query; }
    public String category() { return category; }
    public String era() { return era; }
    public String minimumPrice() { return minimumPrice; }
    public String maximumPrice() { return maximumPrice; }
    public String endingWithin() { return endingWithin; }
    public String sort() { return sort; }
    public String page() { return page; }
    public String size() { return size; }
    public String getQuery() { return query; }
    public String getCategory() { return category; }
    public String getEra() { return era; }
    public String getMinimumPrice() { return minimumPrice; }
    public String getMaximumPrice() { return maximumPrice; }
    public String getEndingWithin() { return endingWithin; }
    public String getSort() { return sort; }
    public String getPage() { return page; }
    public String getSize() { return size; }
}
