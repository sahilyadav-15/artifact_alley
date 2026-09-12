package com.artifactalley.artifact;

import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ArtifactManagementController {
    private final ArtifactService artifactService;

    public ArtifactManagementController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping("/seller/artifacts/new")
    public String submissionForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, Role.SELLER, redirectAttributes)) return accessRedirect(session);
        if (!model.containsAttribute("artifactSubmissionForm")) model.addAttribute("artifactSubmissionForm", new ArtifactSubmissionForm());
        model.addAttribute("categories", Category.values());
        return "artifact-submit";
    }

    @PostMapping("/seller/artifacts")
    public String submit(@Valid @ModelAttribute ArtifactSubmissionForm artifactSubmissionForm, BindingResult bindingResult,
                         Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, Role.SELLER, redirectAttributes)) return accessRedirect(session);
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", Category.values());
            return "artifact-submit";
        }
        SessionUser seller = signedInUser(session);
        artifactService.submit(artifactSubmissionForm.getTitle(), artifactSubmissionForm.getCategory(),
                artifactSubmissionForm.getEra(), artifactSubmissionForm.getStartingPrice(), artifactSubmissionForm.getClosesAt(),
                artifactSubmissionForm.getDescription(), seller.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Artifact submitted for administrator approval.");
        return "redirect:/";
    }

    @GetMapping("/admin/artifacts/pending")
    public String pendingArtifacts(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, Role.ADMIN, redirectAttributes)) return accessRedirect(session);
        model.addAttribute("artifacts", artifactService.findPendingArtifacts());
        return "artifact-pending";
    }

    @PostMapping("/admin/artifacts/{id}/approve")
    public String approve(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hasRole(session, Role.ADMIN, redirectAttributes)) return accessRedirect(session);
        if (artifactService.approve(id)) redirectAttributes.addFlashAttribute("successMessage", "Artifact approved and now live.");
        else redirectAttributes.addFlashAttribute("errorMessage", "That artifact is not awaiting approval.");
        return "redirect:/admin/artifacts/pending";
    }

    private boolean hasRole(HttpSession session, Role role, RedirectAttributes redirectAttributes) {
        SessionUser user = signedInUser(session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to continue.");
            return false;
        }
        if (user.getRole() != role) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your account does not have access to that page.");
            return false;
        }
        return true;
    }

    private String accessRedirect(HttpSession session) {
        return signedInUser(session) == null ? "redirect:/login" : "redirect:/";
    }

    private SessionUser signedInUser(HttpSession session) {
        Object value = session.getAttribute(AuthController.SIGNED_IN_USER);
        return value instanceof SessionUser user ? user : null;
    }
}
