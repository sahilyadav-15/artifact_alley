package com.artifactalley.artifact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactSearchCriteriaTest {
    @Test
    void normalizesBlankValuesDefaultsAndNonPositivePage() {
        ArtifactSearchCriteria criteria = ArtifactSearchCriteria.normalize(request("   ", null, null, null,
                null, null, null, "0", null));
        assertThat(criteria.query()).isNull();
        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.size()).isEqualTo(12);
        assertThat(criteria.sort()).isEqualTo(ArtifactSearchCriteria.SortOption.ENDING_SOON);
    }

    @Test
    void rejectsUnknownAndUnsafeValuesWithFieldNames() {
        assertInvalid(request(null, "unknown", null, null, null, null, null, null, null), "category");
        assertInvalid(request(null, null, null, null, null, "tomorrow", null, null, null), "endingWithin");
        assertInvalid(request(null, null, null, null, null, null, "title", null, null), "sort");
        assertInvalid(request(null, null, null, null, null, null, null, null, "49"), "size");
        assertInvalid(request("x".repeat(101), null, null, null, null, null, null, null, null), "q");
    }

    @Test
    void rejectsInvalidPricesAndRanges() {
        assertInvalid(request(null, null, null, "-1", null, null, null, null, null), "minPrice");
        assertInvalid(request(null, null, null, "10", "9", null, null, null, null), "minPrice");
        assertInvalid(request(null, null, null, "abc", null, null, null, null, null), "minPrice");
    }

    private ArtifactSearchRequest request(String q, String category, String era, String min, String max,
                                          String ending, String sort, String page, String size) {
        return new ArtifactSearchRequest(q, category, era, min, max, ending, sort, page, size);
    }

    private void assertInvalid(ArtifactSearchRequest request, String field) {
        assertThatThrownBy(() -> ArtifactSearchCriteria.normalize(request))
                .isInstanceOf(ArtifactSearchValidationException.class)
                .extracting(exception -> ((ArtifactSearchValidationException) exception).getField())
                .isEqualTo(field);
    }
}
