package com.artifactalley.bid;

import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BidderActivityController {
    private final BidderActivityService activityService;

    public BidderActivityController(BidderActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/account/bids")
    public String activity(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        SessionUser user = signedInUser(session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in with a bidder account to view your bids.");
            return "redirect:/login";
        }
        if (user.getRole() != Role.BIDDER) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only bidder accounts can view bid activity.");
            return "redirect:/";
        }
        model.addAttribute("activity", activityService.findActivity(user.getId()));
        return "bidder-activity";
    }

    private SessionUser signedInUser(HttpSession session) {
        Object value = session.getAttribute(AuthController.SIGNED_IN_USER);
        return value instanceof SessionUser user ? user : null;
    }
}
