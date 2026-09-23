package com.artifactalley.artifact;

import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import com.artifactalley.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArtifactManagementController.class)
class ArtifactManagementControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SellerArtifactService sellers;
    @MockitoBean ArtifactReviewService reviews;
    @MockitoBean ArtifactImageService images;

    @Test
    void guestDashboardRedirectsToLogin() throws Exception {
        mvc.perform(get("/seller/artifacts")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(sellers);
    }

    @Test
    void bidderAndAdministratorCannotUseSellerDashboard() throws Exception {
        mvc.perform(get("/seller/artifacts").sessionAttr(AuthController.SIGNED_IN_USER, session(2L, Role.BIDDER)))
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/seller/artifacts").sessionAttr(AuthController.SIGNED_IN_USER, session(3L, Role.ADMIN)))
                .andExpect(redirectedUrl("/"));
        verifyNoInteractions(sellers);
    }

    @Test
    void dashboardLoadsOnlyAuthenticatedSellerId() throws Exception {
        when(sellers.findOwned(1L)).thenReturn(List.of());
        mvc.perform(get("/seller/artifacts").sessionAttr(AuthController.SIGNED_IN_USER, session(1L, Role.SELLER)))
                .andExpect(status().isOk()).andExpect(view().name("seller-artifacts"));
        verify(sellers).findOwned(1L);
    }

    @Test
    void anotherSellersDetailIsReturnedAsControlledNotFoundRedirect() throws Exception {
        when(sellers.details(99L, 1L)).thenThrow(new ArtifactOperationException("Listing was not found."));
        mvc.perform(get("/seller/artifacts/99").sessionAttr(AuthController.SIGNED_IN_USER, session(1L, Role.SELLER)))
                .andExpect(redirectedUrl("/seller/artifacts")).andExpect(flash().attribute("errorMessage", "Listing was not found."));
    }

    @Test
    void nonAdminCannotApproveOrRejectByDirectUrl() throws Exception {
        mvc.perform(post("/admin/artifacts/10/approve").sessionAttr(AuthController.SIGNED_IN_USER, session(1L, Role.SELLER)))
                .andExpect(redirectedUrl("/"));
        mvc.perform(post("/admin/artifacts/10/reject").param("reason", "No provenance")
                        .sessionAttr(AuthController.SIGNED_IN_USER, session(2L, Role.BIDDER)))
                .andExpect(redirectedUrl("/"));
        verifyNoInteractions(reviews);
    }

    @Test
    void imageMutationAlwaysUsesAuthenticatedSellerId() throws Exception {
        mvc.perform(post("/seller/artifacts/10/images/5/delete")
                        .sessionAttr(AuthController.SIGNED_IN_USER, session(1L, Role.SELLER)))
                .andExpect(redirectedUrl("/seller/artifacts/10"));
        verify(images).delete(10L, 5L, 1L);
    }

    private SessionUser session(Long id, Role role) {
        User user = new User("Test", role.name().toLowerCase() + "@example.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", id); return SessionUser.from(user);
    }
}
