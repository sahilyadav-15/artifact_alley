package com.artifactalley.user;

import com.artifactalley.security.CsrfTokenService;
import com.artifactalley.security.LoginThrottleService;
import com.artifactalley.security.SecurityAuditService;
import com.artifactalley.security.SessionAuthentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    public static final String SIGNED_IN_USER = "signedInUser";
    private final UserService userService;
    private final LoginThrottleService loginThrottle;
    private final CsrfTokenService csrf;
    private final SecurityAuditService audit;
    private final SessionAuthentication authentication;

    public AuthController(UserService userService, LoginThrottleService loginThrottle, CsrfTokenService csrf,
                          SecurityAuditService audit, SessionAuthentication authentication) {
        this.userService = userService;
        this.loginThrottle = loginThrottle;
        this.csrf = csrf;
        this.audit = audit;
        this.authentication = authentication;
    }

    @GetMapping("/register")
    public String registrationForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationForm registrationForm, BindingResult bindingResult,
                           HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return "register";
        try {
            User user = userService.register(registrationForm);
            renewAuthenticatedSession(request, session, user);
            audit.record("REGISTER_SUCCESS", user.getId(), user.getId(), "success");
            redirectAttributes.addFlashAttribute("successMessage", "Welcome to Artifact Alley, " + user.getName() + ".");
            return "redirect:/";
        } catch (EmailAlreadyRegisteredException exception) {
            audit.record("REGISTER_REJECTED", null, null, "duplicate_email");
            bindingResult.rejectValue("email", "duplicate", "An account already uses this email address.");
            return "register";
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("role", "invalid", exception.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        if (!model.containsAttribute("loginForm")) model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm loginForm, BindingResult bindingResult,
                        HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return "login";
        if (loginThrottle.isBlocked(loginForm.getEmail(), request)) {
            audit.record("LOGIN_THROTTLED", null, null, "blocked");
            bindingResult.reject("invalidCredentials", "Sign-in is temporarily unavailable. Please try again later.");
            return "login";
        }
        return userService.authenticate(loginForm.getEmail(), loginForm.getPassword())
                .map(user -> {
                    loginThrottle.recordSuccess(loginForm.getEmail(), request);
                    renewAuthenticatedSession(request, session, user);
                    audit.record("LOGIN_SUCCESS", user.getId(), user.getId(), "success");
                    redirectAttributes.addFlashAttribute("successMessage", "Welcome back, " + user.getName() + ".");
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    loginThrottle.recordFailure(loginForm.getEmail(), request);
                    audit.record("LOGIN_FAILURE", null, null, "invalid_credentials");
                    bindingResult.reject("invalidCredentials", "Email or password is incorrect.");
                    return "login";
                });
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        authentication.currentUser(session).ifPresent(user -> audit.record("LOGOUT", user.getId(), user.getId(), "success"));
        csrf.clear(session);
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "You have been signed out.");
        return "redirect:/";
    }

    @GetMapping("/account/password")
    public String changePasswordForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (authentication.currentUser(session).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to change your password.");
            return "redirect:/login";
        }
        if (!model.containsAttribute("changePasswordForm")) model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "change-password";
    }

    @PostMapping("/account/password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordForm changePasswordForm, BindingResult bindingResult,
                                 HttpSession session, RedirectAttributes redirectAttributes) {
        User user = authentication.currentUser(session).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to change your password.");
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) return "change-password";
        if (!userService.changePassword(user.getId(), changePasswordForm.getCurrentPassword(), changePasswordForm.getNewPassword())) {
            audit.record("PASSWORD_CHANGE_FAILURE", user.getId(), user.getId(), "invalid_current_password");
            bindingResult.rejectValue("currentPassword", "invalid", "Your current password is incorrect.");
            return "change-password";
        }
        audit.record("PASSWORD_CHANGE_SUCCESS", user.getId(), user.getId(), "success");
        redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        return "redirect:/";
    }

    @PostMapping("/account/become-seller")
    public String becomeSeller(HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
        User signedInUser = authentication.currentUser(session).orElse(null);
        if (signedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to become a seller.");
            return "redirect:/login";
        }
        return userService.becomeSeller(signedInUser.getId())
                .map(user -> {
                    renewAuthenticatedSession(request, session, user);
                    audit.record("ROLE_UPGRADE", user.getId(), user.getId(), "seller");
                    redirectAttributes.addFlashAttribute("successMessage", "Your account is now a seller account. You can submit an artifact.");
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    audit.record("ROLE_UPGRADE_REJECTED", signedInUser.getId(), signedInUser.getId(), "invalid_role");
                    redirectAttributes.addFlashAttribute("errorMessage", "Only bidder accounts can be upgraded to seller accounts.");
                    return "redirect:/";
                });
    }

    private void renewAuthenticatedSession(HttpServletRequest request, HttpSession session, User user) {
        request.changeSessionId();
        csrf.rotateToken(session);
        session.setAttribute(SIGNED_IN_USER, SessionUser.from(user));
    }
}
