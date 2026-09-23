package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.artifact.Category;
import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import com.artifactalley.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ArtifactAuctionController.class)
class ArtifactAuctionControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private BiddingService biddingService;
    private AuctionDetails details;
    private SessionUser bidderSession;

    @BeforeEach
    void setUp() {
        Artifact artifact = new Artifact("Test", Category.OTHER, "1900", new BigDecimal("1000.00"),
                LocalDateTime.now().plusHours(1), "Description", "seller@example.com", ArtifactStatus.LIVE);
        ReflectionTestUtils.setField(artifact, "id", 1L);
        details = new AuctionDetails(artifact, new BigDecimal("1100.00"), List.of());
        when(biddingService.getAuctionDetails(1L)).thenReturn(details);
        User user = new User("Bidder", "bidder@example.com", "hash", Role.BIDDER);
        ReflectionTestUtils.setField(user, "id", 7L);
        bidderSession = SessionUser.from(user);
    }

    @Test
    void guestCanViewDetailsButUnauthenticatedPostRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/artifacts/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("artifact-details"))
                .andExpect(model().attribute("canBid", false));

        mockMvc.perform(post("/artifacts/1/bids").param("amount", "1100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void successfulPostUsesRedirectAndAuthenticatedSessionIdentity() throws Exception {
        mockMvc.perform(post("/artifacts/1/bids")
                        .sessionAttr(AuthController.SIGNED_IN_USER, bidderSession)
                        .param("amount", "1100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artifacts/1"))
                .andExpect(flash().attribute("successMessage", "Your bid was placed successfully."));

        verify(biddingService).placeBid(1L, 7L, new BigDecimal("1100.00"));
    }

    @Test
    void validationFailureReturnsDetailsWithEnteredAmount() throws Exception {
        mockMvc.perform(post("/artifacts/1/bids")
                        .sessionAttr(AuthController.SIGNED_IN_USER, bidderSession)
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("artifact-details"))
                .andExpect(model().attributeHasFieldErrors("bidForm", "amount"))
                .andExpect(model().attributeExists("bidForm"));
    }

    @Test
    void missingArtifactReturnsControlled404Page() throws Exception {
        when(biddingService.getAuctionDetails(99L)).thenThrow(new ArtifactNotFoundException());
        mockMvc.perform(get("/artifacts/99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("not-found"))
                .andExpect(model().attribute("errorMessage", "That artifact could not be found."));
    }
}
