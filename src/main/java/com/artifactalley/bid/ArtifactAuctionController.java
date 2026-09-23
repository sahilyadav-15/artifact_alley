package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class ArtifactAuctionController {
    private final BiddingService biddingService;

    public ArtifactAuctionController(BiddingService biddingService) {
        this.biddingService = biddingService;
    }

    @GetMapping("/artifacts/{id}")
    public String details(@PathVariable Long id, Model model, HttpSession session) {
        if (!model.containsAttribute("bidForm")) model.addAttribute("bidForm", new BidForm());
        populateDetails(id, model, session);
        return "artifact-details";
    }

    @PostMapping("/artifacts/{id}/bids")
    public String placeBid(@PathVariable Long id, @Valid @ModelAttribute BidForm bidForm,
                           BindingResult bindingResult, Model model, HttpSession session,
                           RedirectAttributes redirectAttributes) {
        SessionUser signedInUser = signedInUser(session);
        if (signedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in with a bidder account to place a bid.");
            return "redirect:/login";
        }
        if (signedInUser.getRole() != Role.BIDDER) {
            populateDetails(id, model, session);
            model.addAttribute("bidError", "Only bidder accounts can place bids.");
            return "artifact-details";
        }
        if (bindingResult.hasErrors()) {
            populateDetails(id, model, session);
            return "artifact-details";
        }

        try {
            biddingService.placeBid(id, signedInUser.getId(), bidForm.getAmount());
            redirectAttributes.addFlashAttribute("successMessage", "Your bid was placed successfully.");
            return "redirect:/artifacts/" + id;
        } catch (BiddingException exception) {
            populateDetails(id, model, session);
            model.addAttribute("bidError", exception.getMessage());
            return "artifact-details";
        } catch (TransientDataAccessException exception) {
            populateDetails(id, model, session);
            model.addAttribute("bidError", "Another bid was placed at the same time. Review the latest price and try again.");
            return "artifact-details";
        }
    }

    private void populateDetails(Long id, Model model, HttpSession session) {
        AuctionDetails details = biddingService.getAuctionDetails(id);
        Artifact artifact = details.getArtifact();
        boolean open = artifact.getStatus() == ArtifactStatus.LIVE && artifact.getClosesAt().isAfter(LocalDateTime.now());
        SessionUser signedInUser = signedInUser(session);
        model.addAttribute("details", details);
        model.addAttribute("auctionOpen", open);
        model.addAttribute("canBid", open && signedInUser != null && signedInUser.getRole() == Role.BIDDER);
        model.addAttribute("auctionState", open ? "LIVE" : artifact.getStatus() == ArtifactStatus.LIVE ? "CLOSED" : artifact.getStatus());
    }

    private SessionUser signedInUser(HttpSession session) {
        Object value = session.getAttribute(AuthController.SIGNED_IN_USER);
        return value instanceof SessionUser user ? user : null;
    }
}
