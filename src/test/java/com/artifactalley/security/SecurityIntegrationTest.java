package com.artifactalley.security;

import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import com.artifactalley.user.UserService;
import com.artifactalley.user.RegistrationForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserService userService;

    @Test
    void registrationAndRoleUpgradeRotateSessionIdAndCsrfToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/register").session(session)).andExpect(status().isOk());
        String initialId = session.getId();
        String initialToken = token(session);

        mvc.perform(post("/register").session(session).param("_csrf", initialToken)
                        .param("name", "Rotation Test").param("email", "rotation@test.invalid")
                        .param("password", "secret12").param("confirmPassword", "secret12").param("role", "BIDDER"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));

        assertThat(session.getId()).isNotEqualTo(initialId);
        assertThat(session.getAttribute(AuthController.SIGNED_IN_USER)).isInstanceOf(SessionUser.class);
        String authenticatedToken = token(session);
        assertThat(authenticatedToken).isNotEqualTo(initialToken);

        mvc.perform(post("/account/become-seller").session(session).param("_csrf", initialToken))
                .andExpect(status().isForbidden());
        String authenticatedId = session.getId();
        mvc.perform(post("/account/become-seller").session(session).param("_csrf", authenticatedToken))
                .andExpect(status().is3xxRedirection());
        assertThat(session.getId()).isNotEqualTo(authenticatedId);
        assertThat(token(session)).isNotEqualTo(authenticatedToken);
    }

    @Test
    void browserCsrfRejectsMissingAndWrongTokensAndLogoutInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/login").session(session)).andExpect(status().isOk());
        String token = token(session);
        mvc.perform(post("/login").session(session)).andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/WEB-INF/jsp/access-denied.jsp"));
        mvc.perform(post("/login").session(session).param("_csrf", "wrong"))
                .andExpect(status().isForbidden());

        User bidder = users.save(new User("Logout Test", "logout@test.invalid", "unusable", Role.BIDDER));
        session.setAttribute(AuthController.SIGNED_IN_USER, SessionUser.from(bidder));
        mvc.perform(post("/logout").session(session).param("_csrf", token))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void restCsrfIsStructuredAndTokensCannotCrossSessions() throws Exception {
        MockHttpSession first = new MockHttpSession();
        MockHttpSession second = new MockHttpSession();
        mvc.perform(get("/api/v1/session/csrf").session(first).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value("X-CSRF-Token"))
                .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/api/v1/session/csrf").session(second)).andExpect(status().isOk());

        mvc.perform(post("/api/v1/artifacts/1/bids").session(first)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":200}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/api/v1/artifacts/1/bids").session(second)
                        .header("X-CSRF-Token", token(first)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/api/v1/artifacts/1/bids").session(first)
                        .header("X-CSRF-Token", token(first)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void safeRequestsHaveSecurityHeadersAndDatabaseRoleOverridesSessionRole() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("script-src 'self' 'nonce-")));

        User bidder = users.save(new User("Stale Role", "stale-role@test.invalid", "hash", Role.BIDDER));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.SIGNED_IN_USER, forged(bidder, Role.ADMIN));
        mvc.perform(get("/api/v1/admin/reports/auction-summary").session(session))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("INSUFFICIENT_ROLE"));
    }

    @Test
    void browserLoginUsesGenericFailureAndThrottlesCorrectCredentialsAtThreshold() throws Exception {
        RegistrationForm form = new RegistrationForm();
        form.setName("Throttle User"); form.setEmail("throttle-user@test.invalid");
        form.setPassword("secret12"); form.setConfirmPassword("secret12"); form.setRole(Role.BIDDER);
        userService.register(form);
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/login").session(session)).andExpect(status().isOk());
        String csrf = token(session);

        String knownFailure = loginFailure(session, csrf, "throttle-user@test.invalid", "wrong-password");
        String unknownFailure = loginFailure(session, csrf, "unknown-user@test.invalid", "wrong-password");
        assertThat(knownFailure).isEqualTo("Email or password is incorrect.");
        assertThat(unknownFailure).isEqualTo(knownFailure);
        for (int count = 1; count < 5; count++) {
            loginFailure(session, csrf, "THROTTLE-USER@test.invalid", "wrong-password");
        }
        assertThat(loginFailure(session, csrf, "throttle-user@test.invalid", "secret12"))
                .isEqualTo("Sign-in is temporarily unavailable. Please try again later.");
    }

    private SessionUser forged(User user, Role role) {
        User forged = new User(user.getName(), user.getEmail(), "hash", role);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(forged, user.getId());
            return SessionUser.from(forged);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private String token(MockHttpSession session) {
        return (String) session.getAttribute(CsrfTokenService.SESSION_ATTRIBUTE);
    }

    private String loginFailure(MockHttpSession session, String csrf, String email, String password) throws Exception {
        var result = mvc.perform(post("/login").session(session).param("_csrf", csrf)
                        .param("email", email).param("password", password))
                .andExpect(status().isOk()).andExpect(view().name("login")).andReturn();
        BindingResult binding = (BindingResult) result.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "loginForm");
        return binding.getGlobalError().getDefaultMessage();
    }
}
