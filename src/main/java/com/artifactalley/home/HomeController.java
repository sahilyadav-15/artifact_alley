package com.artifactalley.home;

import com.artifactalley.artifact.ArtifactService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ArtifactService artifactService;

    public HomeController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("artifacts", artifactService.findLiveArtifacts());
        return "home";
    }
}
