package com.artifactalley.user;

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

    public AuthController(UserService userService) {
        this.userService = userService;
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
                           HttpSession session, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return "register";
        try {
            User user = userService.register(registrationForm);
            session.setAttribute(SIGNED_IN_USER, SessionUser.from(user));
            redirectAttributes.addFlashAttribute("successMessage", "Welcome to Artifact Alley, " + user.getName() + ".");
            return "redirect:/";
        } catch (EmailAlreadyRegisteredException exception) {
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
                        HttpSession session, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return "login";
        return userService.authenticate(loginForm.getEmail(), loginForm.getPassword())
                .map(user -> {
                    session.setAttribute(SIGNED_IN_USER, SessionUser.from(user));
                    redirectAttributes.addFlashAttribute("successMessage", "Welcome back, " + user.getName() + ".");
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    bindingResult.reject("invalidCredentials", "Email or password is incorrect.");
                    return "login";
                });
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "You have been signed out.");
        return "redirect:/";
    }

    @GetMapping("/account/password")
    public String changePasswordForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (signedInUser(session) == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to change your password.");
            return "redirect:/login";
        }
        if (!model.containsAttribute("changePasswordForm")) model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "change-password";
    }

    @PostMapping("/account/password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordForm changePasswordForm, BindingResult bindingResult,
                                 HttpSession session, RedirectAttributes redirectAttributes) {
        SessionUser user = signedInUser(session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to change your password.");
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) return "change-password";
        if (!userService.changePassword(user.getId(), changePasswordForm.getCurrentPassword(), changePasswordForm.getNewPassword())) {
            bindingResult.rejectValue("currentPassword", "invalid", "Your current password is incorrect.");
            return "change-password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        return "redirect:/";
    }

    @PostMapping("/account/become-seller")
    public String becomeSeller(HttpSession session, RedirectAttributes redirectAttributes) {
        SessionUser signedInUser = signedInUser(session);
        if (signedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to become a seller.");
            return "redirect:/login";
        }
        return userService.becomeSeller(signedInUser.getId())
                .map(user -> {
                    session.setAttribute(SIGNED_IN_USER, SessionUser.from(user));
                    redirectAttributes.addFlashAttribute("successMessage", "Your account is now a seller account. You can submit an artifact.");
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Only bidder accounts can be upgraded to seller accounts.");
                    return "redirect:/";
                });
    }

    private SessionUser signedInUser(HttpSession session) {
        Object value = session.getAttribute(SIGNED_IN_USER);
        return value instanceof SessionUser user ? user : null;
    }
}
