package com.artifactalley.bid;

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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BidderActivityController.class)
class BidderActivityControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private BidderActivityService activityService;
    private SessionUser bidder;
    private SessionUser seller;

    @BeforeEach
    void setUp() {
        bidder = sessionUser(7L, Role.BIDDER);
        seller = sessionUser(8L, Role.SELLER);
        when(activityService.findActivity(7L)).thenReturn(new BidderActivity(List.of(), List.of(), List.of()));
    }

    @Test
    void guestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/account/bids"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void sellerIsRedirectedHome() throws Exception {
        mockMvc.perform(get("/account/bids").sessionAttr(AuthController.SIGNED_IN_USER, seller))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void bidderReceivesSeparatedActivityModel() throws Exception {
        mockMvc.perform(get("/account/bids").sessionAttr(AuthController.SIGNED_IN_USER, bidder))
                .andExpect(status().isOk())
                .andExpect(view().name("bidder-activity"))
                .andExpect(model().attributeExists("activity"));
    }

    private SessionUser sessionUser(Long id, Role role) {
        User user = new User(role.name(), role.name().toLowerCase() + "@example.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return SessionUser.from(user);
    }
}
