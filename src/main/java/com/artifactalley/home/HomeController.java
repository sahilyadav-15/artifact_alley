package com.artifactalley.home;

import com.artifactalley.artifact.ArtifactService;
import com.artifactalley.artifact.ArtifactImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ArtifactService artifactService;
    @Autowired(required = false)
    private ArtifactImageService imageService;

    public HomeController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping("/")
    public String home(Model model) {
        var artifacts = artifactService.findLiveArtifacts();
        model.addAttribute("artifacts", artifacts);
        java.util.Map<Long, String> coverUrls = new java.util.LinkedHashMap<>();
        if (imageService != null) artifacts.forEach(artifact -> coverUrls.put(artifact.getId(), imageService.coverUrl(artifact.getId())));
        model.addAttribute("coverImageUrls", coverUrls);
        return "home";
    }
}
