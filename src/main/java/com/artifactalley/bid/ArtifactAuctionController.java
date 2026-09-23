package com.artifactalley.bid;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.artifact.ArtifactImageService;
import org.springframework.beans.factory.annotation.Autowired;
import com.artifactalley.auction.AuctionNotExpiredException;
import com.artifactalley.auction.AuctionSettlementService;
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
import java.time.Clock;

@Controller
public class ArtifactAuctionController {
    private final BiddingService biddingService;
    private final AuctionSettlementService settlementService;
    private final Clock clock;
    @Autowired(required = false)
    private ArtifactImageService imageService;

    public ArtifactAuctionController(BiddingService biddingService,
                                     AuctionSettlementService settlementService,
                                     Clock clock) {
        this.biddingService = biddingService;
        this.settlementService = settlementService;
        this.clock = clock;
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
        try {
            settlementService.settleAuction(id);
        } catch (AuctionNotExpiredException ignored) {
            // Active auctions are expected to reach this branch.
        }
        AuctionDetails details = biddingService.getAuctionDetails(id);
        Artifact artifact = details.getArtifact();
        boolean open = artifact.getStatus() == ArtifactStatus.LIVE
                && artifact.getClosesAt().isAfter(LocalDateTime.now(clock));
        SessionUser signedInUser = signedInUser(session);
        model.addAttribute("details", details);
        model.addAttribute("auctionOpen", open);
        model.addAttribute("canBid", open && signedInUser != null && signedInUser.getRole() == Role.BIDDER);
        model.addAttribute("auctionState", open ? "LIVE" : artifact.getStatus() == ArtifactStatus.LIVE ? "CLOSED" : artifact.getStatus());
        model.addAttribute("signedInBidderWon", signedInUser != null
                && signedInUser.getRole() == Role.BIDDER
                && signedInUser.getId().equals(details.getWinningBidderId()));
        if (imageService != null) {
            var images = imageService.list(id);
            java.util.Map<Long, String> urls = new java.util.LinkedHashMap<>();
            images.forEach(image -> urls.put(image.getId(), imageService.url(image)));
            model.addAttribute("artifactImages", images);
            model.addAttribute("imageUrls", urls);
        }
    }

    private SessionUser signedInUser(HttpSession session) {
        Object value = session.getAttribute(AuthController.SIGNED_IN_USER);
        return value instanceof SessionUser user ? user : null;
    }
}
