package com.artifactalley.home;

import com.artifactalley.artifact.ArtifactSearchRequest;
import com.artifactalley.artifact.ArtifactSearchResult;
import com.artifactalley.artifact.ArtifactSearchService;
import com.artifactalley.artifact.ArtifactSearchValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ArtifactSearchService searchService;

    @Test
    void bookmarkableParametersArePassedToTheSharedSearchService() throws Exception {
        ArtifactSearchResult result = mock(ArtifactSearchResult.class);
        when(searchService.searchActiveArtifacts(any())).thenReturn(result);

        mvc.perform(get("/").param("q", "coins & manuscripts").param("category", "COIN")
                        .param("era", "18th century").param("minPrice", "100").param("maxPrice", "5000")
                        .param("endingWithin", "7d").param("sort", "price-asc").param("page", "2").param("size", "24"))
                .andExpect(status().isOk()).andExpect(view().name("home"))
                .andExpect(model().attribute("searchResult", result))
                .andExpect(model().attributeExists("searchRequest", "categories"));

        ArgumentCaptor<ArtifactSearchRequest> captor = ArgumentCaptor.forClass(ArtifactSearchRequest.class);
        verify(searchService).searchActiveArtifacts(captor.capture());
        assertThat(captor.getValue().query()).isEqualTo("coins & manuscripts");
        assertThat(captor.getValue().page()).isEqualTo("2");
        assertThat(captor.getValue().size()).isEqualTo("24");
    }

    @Test
    void invalidFilterReturnsTheHomeViewWithAFieldLinkedMessage() throws Exception {
        ArtifactSearchResult fallback = mock(ArtifactSearchResult.class);
        when(searchService.searchActiveArtifacts(any()))
                .thenThrow(new ArtifactSearchValidationException("minPrice", "Minimum price cannot exceed maximum price."))
                .thenReturn(fallback);

        mvc.perform(get("/").param("minPrice", "900").param("maxPrice", "100"))
                .andExpect(status().isOk()).andExpect(view().name("home"))
                .andExpect(model().attribute("searchError", "Minimum price cannot exceed maximum price."))
                .andExpect(model().attribute("invalidField", "minPrice"))
                .andExpect(model().attribute("searchResult", fallback));
        verify(searchService, times(2)).searchActiveArtifacts(any());
    }
}
