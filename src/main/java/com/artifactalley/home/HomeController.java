package com.artifactalley.home;

import com.artifactalley.artifact.ArtifactSearchRequest;
import com.artifactalley.artifact.ArtifactSearchService;
import com.artifactalley.artifact.ArtifactSearchValidationException;
import com.artifactalley.artifact.Category;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    private final ArtifactSearchService searchService;

    public HomeController(ArtifactSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String era,
                       @RequestParam(required = false) String minPrice,
                       @RequestParam(required = false) String maxPrice,
                       @RequestParam(required = false) String endingWithin,
                       @RequestParam(required = false) String sort,
                       @RequestParam(required = false) String page,
                       @RequestParam(required = false) String size,
                       Model model) {
        ArtifactSearchRequest request = new ArtifactSearchRequest(query, category, era, minPrice, maxPrice,
                endingWithin, sort, page, size);
        try {
            model.addAttribute("searchResult", searchService.searchActiveArtifacts(request));
        } catch (ArtifactSearchValidationException exception) {
            model.addAttribute("searchError", exception.getMessage());
            model.addAttribute("invalidField", exception.getField());
            model.addAttribute("searchResult", searchService.searchActiveArtifacts(
                    new ArtifactSearchRequest(null, null, null, null, null, null, null, null, null)));
        }
        model.addAttribute("searchRequest", request);
        model.addAttribute("categories", Category.values());
        return "home";
    }
}
