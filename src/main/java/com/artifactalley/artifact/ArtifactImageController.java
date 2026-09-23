package com.artifactalley.artifact;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class ArtifactImageController {
    private final ArtifactImageService imageService;

    public ArtifactImageController(ArtifactImageService imageService) { this.imageService = imageService; }

    @GetMapping("/artifact-images/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> image(@PathVariable Long id) {
        ArtifactImageService.ImageContent content = imageService.open(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(content.resource());
    }
}
