package com.artifactalley.artifact;

import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ArtifactManagementController {
    private final SellerArtifactService sellerService;
    private final ArtifactReviewService reviewService;
    private final ArtifactImageService imageService;

    public ArtifactManagementController(SellerArtifactService sellerService, ArtifactReviewService reviewService,
                                        ArtifactImageService imageService) {
        this.sellerService = sellerService; this.reviewService = reviewService; this.imageService = imageService;
    }

    @GetMapping("/seller/artifacts")
    public String dashboard(Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        List<Artifact> artifacts = sellerService.findOwned(signedInUser(session).getId());
        model.addAttribute("artifacts", artifacts); model.addAttribute("coverImageUrls", coverUrls(artifacts));
        return "seller-artifacts";
    }

    @GetMapping("/seller/artifacts/new")
    public String submissionForm(Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        if (!model.containsAttribute("artifactSubmissionForm")) model.addAttribute("artifactSubmissionForm", new ArtifactSubmissionForm());
        model.addAttribute("categories", Category.values()); return "artifact-submit";
    }

    @PostMapping("/seller/artifacts")
    public String submit(@Valid @ModelAttribute ArtifactSubmissionForm artifactSubmissionForm, BindingResult errors,
                         @RequestParam(name = "images", required = false) List<MultipartFile> images,
                         Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        if (!errors.hasErrors()) {
            try {
                Artifact artifact = sellerService.submit(artifactSubmissionForm, signedInUser(session).getId(), images);
                redirect.addFlashAttribute("successMessage", "Artifact submitted for administrator approval.");
                return "redirect:/seller/artifacts/" + artifact.getId();
            } catch (ArtifactOperationException exception) { errors.reject("images", exception.getMessage()); }
        }
        model.addAttribute("categories", Category.values()); return "artifact-submit";
    }

    @GetMapping("/seller/artifacts/{id}")
    public String sellerDetails(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        try {
            model.addAttribute("details", sellerService.details(id, signedInUser(session).getId())); addImages(model, id);
            return "seller-artifact-details";
        } catch (ArtifactOperationException exception) {
            redirect.addFlashAttribute("errorMessage", "Listing was not found."); return "redirect:/seller/artifacts";
        }
    }

    @GetMapping("/seller/artifacts/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        try {
            Artifact artifact = sellerService.details(id, signedInUser(session).getId()).artifact();
            if (artifact.getStatus() != ArtifactStatus.PENDING_APPROVAL && artifact.getStatus() != ArtifactStatus.REJECTED) {
                redirect.addFlashAttribute("errorMessage", "This listing is read-only in its current state."); return "redirect:/seller/artifacts/" + id;
            }
            if (!model.containsAttribute("artifactSubmissionForm")) model.addAttribute("artifactSubmissionForm", sellerService.editForm(artifact));
            model.addAttribute("artifact", artifact); model.addAttribute("categories", Category.values()); addImages(model, id);
            return "artifact-edit";
        } catch (ArtifactOperationException exception) {
            redirect.addFlashAttribute("errorMessage", "Listing was not found."); return "redirect:/seller/artifacts";
        }
    }

    @PostMapping("/seller/artifacts/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute ArtifactSubmissionForm artifactSubmissionForm,
                       BindingResult errors, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        if (errors.hasErrors()) {
            try { model.addAttribute("artifact", sellerService.details(id, signedInUser(session).getId()).artifact()); addImages(model, id); }
            catch (ArtifactOperationException exception) { return "redirect:/seller/artifacts"; }
            model.addAttribute("categories", Category.values()); return "artifact-edit";
        }
        return sellerAction(id, redirect, () -> sellerService.edit(id, signedInUser(session).getId(), artifactSubmissionForm), "Listing details updated.");
    }

    @PostMapping("/seller/artifacts/{id}/resubmit")
    public String resubmit(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        return sellerAction(id, redirect, () -> sellerService.resubmit(id, signedInUser(session).getId()), "Listing resubmitted for review.");
    }

    @PostMapping("/seller/artifacts/{id}/withdraw")
    public String withdraw(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        return sellerAction(id, redirect, () -> sellerService.withdraw(id, signedInUser(session).getId()), "Listing withdrawn.");
    }

    @PostMapping("/seller/artifacts/{id}/images")
    public String uploadImages(@PathVariable Long id, @RequestParam("images") List<MultipartFile> uploads,
                               HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        return sellerAction(id, redirect, () -> imageService.upload(id, signedInUser(session).getId(), uploads), "Photos uploaded.");
    }

    @PostMapping("/seller/artifacts/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id, @PathVariable Long imageId, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        return sellerAction(id, redirect, () -> imageService.delete(id, imageId, signedInUser(session).getId()), "Photo deleted.");
    }

    @PostMapping("/seller/artifacts/{id}/images/{imageId}/cover")
    public String coverImage(@PathVariable Long id, @PathVariable Long imageId, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        return sellerAction(id, redirect, () -> imageService.selectCover(id, imageId, signedInUser(session).getId()), "Cover photo updated.");
    }

    @PostMapping("/seller/artifacts/{id}/images/reorder")
    public String reorderImages(@PathVariable Long id, @RequestParam("imageIds") List<Long> imageIds,
                                HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.SELLER, redirect)) return accessRedirect(session);
        return sellerAction(id, redirect, () -> imageService.reorder(id, signedInUser(session).getId(), imageIds), "Photo order updated.");
    }

    @GetMapping("/admin/artifacts/pending")
    public String pendingArtifacts(Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.ADMIN, redirect)) return accessRedirect(session);
        List<Artifact> artifacts = reviewService.pending(); model.addAttribute("artifacts", artifacts);
        model.addAttribute("coverImageUrls", coverUrls(artifacts)); return "artifact-pending";
    }

    @GetMapping("/admin/artifacts/{id}")
    public String review(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.ADMIN, redirect)) return accessRedirect(session);
        try {
            model.addAttribute("artifact", reviewService.pendingDetail(id)); model.addAttribute("artifactReviewForm", new ArtifactReviewForm());
            addImages(model, id); return "artifact-review";
        } catch (ArtifactOperationException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage()); return "redirect:/admin/artifacts/pending";
        }
    }

    @PostMapping("/admin/artifacts/{id}/approve")
    public String approve(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.ADMIN, redirect)) return accessRedirect(session);
        try { reviewService.approve(id, signedInUser(session).getId()); redirect.addFlashAttribute("successMessage", "Artifact approved and now live."); }
        catch (ArtifactOperationException exception) { redirect.addFlashAttribute("errorMessage", exception.getMessage()); }
        return "redirect:/admin/artifacts/pending";
    }

    @PostMapping("/admin/artifacts/{id}/reject")
    public String reject(@PathVariable Long id, @Valid @ModelAttribute ArtifactReviewForm form, BindingResult errors,
                         HttpSession session, RedirectAttributes redirect) {
        if (!hasRole(session, Role.ADMIN, redirect)) return accessRedirect(session);
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", errors.getAllErrors().getFirst().getDefaultMessage()); return "redirect:/admin/artifacts/" + id;
        }
        try { reviewService.reject(id, signedInUser(session).getId(), form.getReason()); redirect.addFlashAttribute("successMessage", "Artifact rejected with feedback for the seller."); }
        catch (ArtifactOperationException | IllegalArgumentException exception) { redirect.addFlashAttribute("errorMessage", exception.getMessage()); }
        return "redirect:/admin/artifacts/pending";
    }

    private String sellerAction(Long id, RedirectAttributes redirect, Runnable action, String success) {
        try { action.run(); redirect.addFlashAttribute("successMessage", success); }
        catch (ArtifactOperationException | IllegalStateException exception) { redirect.addFlashAttribute("errorMessage", exception.getMessage()); }
        return "redirect:/seller/artifacts/" + id;
    }

    private void addImages(Model model, Long artifactId) {
        List<ArtifactImage> images = imageService.list(artifactId); Map<Long, String> urls = new LinkedHashMap<>();
        images.forEach(image -> urls.put(image.getId(), imageService.url(image)));
        model.addAttribute("artifactImages", images); model.addAttribute("imageUrls", urls);
    }
    private Map<Long, String> coverUrls(List<Artifact> artifacts) {
        Map<Long, String> urls = new LinkedHashMap<>(); artifacts.forEach(artifact -> urls.put(artifact.getId(), imageService.coverUrl(artifact.getId()))); return urls;
    }
    private boolean hasRole(HttpSession session, Role role, RedirectAttributes redirect) {
        SessionUser user = signedInUser(session);
        if (user == null) { redirect.addFlashAttribute("errorMessage", "Please sign in to continue."); return false; }
        if (user.getRole() != role) { redirect.addFlashAttribute("errorMessage", "Your account does not have access to that page."); return false; }
        return true;
    }
    private String accessRedirect(HttpSession session) { return signedInUser(session) == null ? "redirect:/login" : "redirect:/"; }
    private SessionUser signedInUser(HttpSession session) {
        Object value = session.getAttribute(AuthController.SIGNED_IN_USER); return value instanceof SessionUser user ? user : null;
    }
}
