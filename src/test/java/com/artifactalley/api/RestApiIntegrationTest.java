package com.artifactalley.api;

import com.artifactalley.artifact.*;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RestApiIntegrationTest {
    private static final String CSRF = "integration-test-csrf-token";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ArtifactRepository artifacts;

    private User bidder;
    private User seller;
    private User admin;
    private Artifact live;

    @BeforeEach
    void setUp() {
        String suffix = Long.toHexString(System.nanoTime());
        bidder = users.save(new User("API Bidder", "bidder-" + suffix + "@test.invalid", "hash", Role.BIDDER));
        seller = users.save(new User("API Seller", "seller-" + suffix + "@test.invalid", "hash", Role.SELLER));
        admin = users.save(new User("API Admin", "admin-" + suffix + "@test.invalid", "hash", Role.ADMIN));
        live = artifacts.save(new Artifact("API bronze astrolabe", Category.OTHER, "1800s", new BigDecimal("500.00"),
                LocalDateTime.now().plusDays(3), "A public API integration fixture.", seller, seller.getEmail(),
                ArtifactStatus.LIVE, LocalDateTime.now()));
    }

    @Test
    void publicRepresentationsAreDtoOnlyAndStructuredErrorsNegotiate() throws Exception {
        mvc.perform(get("/api/v1/artifacts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[*].id", hasItem(live.getId().intValue())))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("storageKey"))));

        mvc.perform(get("/api/v1/artifacts/{id}", live.getId()).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/ArtifactView/id").string(live.getId().toString()))
                .andExpect(xpath("/ArtifactView/title").string(live.getTitle()));

        mvc.perform(get("/api/v1/artifacts/{id}", Long.MAX_VALUE).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        mvc.perform(csrfPost("/api/v1/artifacts/{id}/bids", live.getId()).session(session(bidder))
                        .contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML)
                        .content("<BidRequest><amount>0</amount></BidRequest>"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/error/code").string("VALIDATION_FAILED"))
                .andExpect(xpath("/error/fieldErrors/fieldError/field").string("amount"));
    }

    @Test
    void pageLimitMediaTypeAndMalformedBodyAreHandled() throws Exception {
        mvc.perform(get("/api/v1/artifacts?size=51")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEARCH_FILTER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        mvc.perform(csrfPost("/api/v1/artifacts/{id}/bids", live.getId()).session(session(bidder))
                        .contentType(MediaType.TEXT_PLAIN).content("600"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(csrfPost("/api/v1/artifacts/{id}/bids", live.getId()).session(session(bidder))
                        .contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mvc.perform(get("/api/v1/artifacts").accept("application/yaml"))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void discoveryFiltersSerializeInJsonAndXmlAndRejectInvalidValues() throws Exception {
        mvc.perform(get("/api/v1/artifacts").param("q", "bronze").param("category", "other")
                        .param("minPrice", "400").param("maxPrice", "600").param("endingWithin", "7d")
                        .param("sort", "price-desc").param("page", "1").param("size", "12")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(live.getId()))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.sort").value("price-desc"))
                .andExpect(jsonPath("$.appliedFilters.q").value("bronze"));

        mvc.perform(get("/api/v1/artifacts").param("q", "bronze").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/artifacts/artifact/id").string(live.getId().toString()))
                .andExpect(xpath("/artifacts/page").string("1"));

        mvc.perform(get("/api/v1/artifacts").param("minPrice", "900").param("maxPrice", "100"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SEARCH_FILTER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("minPrice"));
        mvc.perform(get("/api/v1/artifacts").param("sort", "seller.email"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SEARCH_FILTER"));
    }

    @Test
    void protectedRoutesUseSessionRolesWithoutRedirects() throws Exception {
        mvc.perform(get("/api/v1/seller/artifacts"))
                .andExpect(status().isUnauthorized()).andExpect(redirectedUrl(null))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mvc.perform(get("/api/v1/admin/reports/auction-summary").session(session(bidder)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));
    }

    @Test
    void bidderSellerAndAdministratorReuseDomainServices() throws Exception {
        mvc.perform(csrfPost("/api/v1/artifacts/{id}/bids", live.getId()).session(session(bidder))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":600.00}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/artifacts/" + live.getId() + "/bids"))
                .andExpect(jsonPath("$.currentPrice").value(600.0));
        mvc.perform(csrfPost("/api/v1/artifacts/{id}/bids", live.getId()).session(session(bidder))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":650.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BID_BELOW_MINIMUM"));
        mvc.perform(csrfPost("/api/v1/seller/artifacts/{id}/withdraw", live.getId()).session(session(seller)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LISTING_CONFLICT"));

        User anotherSeller = users.save(new User("Another Seller", "other-" + Long.toHexString(System.nanoTime())
                + "@test.invalid", "hash", Role.SELLER));
        mvc.perform(get("/api/v1/seller/artifacts/{id}", live.getId()).session(session(anotherSeller)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        String request = """
                {"title":"API seller submission","category":"COIN","era":"1900s","startingPrice":250.00,
                 "closesAt":"%s","description":"Submitted through the seller REST API."}
                """.formatted(LocalDateTime.now().plusDays(4));
        mvc.perform(csrfPost("/api/v1/seller/artifacts").session(session(seller))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        Artifact pending = artifacts.save(new Artifact("Pending API vase", Category.SCULPTURE, "1850",
                new BigDecimal("800"), LocalDateTime.now().plusDays(2), "Ready for review.", seller,
                seller.getEmail(), ArtifactStatus.PENDING_APPROVAL, LocalDateTime.now()));
        mvc.perform(csrfPost("/api/v1/admin/artifacts/{id}/approve", pending.getId()).session(session(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LIVE"));

        Artifact rejected = artifacts.save(new Artifact("Pending API painting", Category.PAINTING, "1920",
                new BigDecimal("900"), LocalDateTime.now().plusDays(2), "Ready for review.", seller,
                seller.getEmail(), ArtifactStatus.PENDING_APPROVAL, LocalDateTime.now()));
        mvc.perform(csrfPost("/api/v1/admin/artifacts/{id}/reject", rejected.getId()).session(session(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Provenance document required.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void openApiContainsVersionedRoutesAndNoUserEntitySecrets() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/artifacts']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/artifacts'].get.parameters[*].name",
                        hasItems("q", "category", "era", "minPrice", "maxPrice", "endingWithin", "sort", "page", "size")))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    private MockHttpSession session(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("signedInUser", SessionUser.from(user));
        session.setAttribute("csrfToken", CSRF);
        return session;
    }

    private MockHttpServletRequestBuilder csrfPost(String url, Object... variables) {
        return post(url, variables).header("X-CSRF-Token", CSRF);
    }
}
