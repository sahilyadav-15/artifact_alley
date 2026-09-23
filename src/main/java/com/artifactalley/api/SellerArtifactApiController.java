package com.artifactalley.api;

import com.artifactalley.artifact.Artifact;
import com.artifactalley.artifact.ArtifactSubmissionForm;
import com.artifactalley.artifact.SellerArtifactService;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/seller/artifacts", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Seller artifacts")
@SecurityRequirement(name = "sessionCookie")
public class SellerArtifactApiController {
    private final SellerArtifactService sellerService;
    private final ArtifactApiReadService readService;
    private final ApiSessionUserResolver sessions;

    public SellerArtifactApiController(SellerArtifactService sellerService, ArtifactApiReadService readService,
                                       ApiSessionUserResolver sessions) {
        this.sellerService = sellerService;
        this.readService = readService;
        this.sessions = sessions;
    }

    @GetMapping
    @Operation(summary = "List the authenticated seller's artifacts")
    public ApiDtos.SellerArtifactList list(HttpSession session) {
        SessionUser user = seller(session);
        return new ApiDtos.SellerArtifactList(sellerService.findOwned(user.getId()).stream()
                .map(readService::sellerView).toList());
    }

    @GetMapping("/{id}")
    public ApiDtos.SellerArtifactView details(@PathVariable Long id, HttpSession session) {
        SessionUser user = seller(session);
        return readService.sellerView(sellerService.details(id, user.getId()).artifact());
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Submit an artifact for approval")
    public ResponseEntity<ApiDtos.SellerArtifactView> create(@Valid @RequestBody ApiDtos.ArtifactRequest request,
                                                              HttpSession session) {
        SessionUser user = seller(session);
        Artifact artifact = sellerService.submit(form(request), user.getId(), List.of());
        URI location = URI.create("/api/v1/seller/artifacts/" + artifact.getId());
        return ResponseEntity.created(location).body(readService.sellerView(artifact));
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Update a pending or rejected artifact")
    public ApiDtos.SellerArtifactView update(@PathVariable Long id,
                                              @Valid @RequestBody ApiDtos.ArtifactRequest request,
                                              HttpSession session) {
        SessionUser user = seller(session);
        sellerService.edit(id, user.getId(), form(request));
        return readService.sellerView(sellerService.details(id, user.getId()).artifact());
    }

    @PostMapping("/{id}/resubmit")
    @Operation(summary = "Resubmit a rejected artifact")
    public ApiDtos.SellerArtifactView resubmit(@PathVariable Long id, HttpSession session) {
        SessionUser user = seller(session);
        sellerService.resubmit(id, user.getId());
        return readService.sellerView(sellerService.details(id, user.getId()).artifact());
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw without deleting auction history")
    public ResponseEntity<Void> withdraw(@PathVariable Long id, HttpSession session) {
        SessionUser user = seller(session);
        sellerService.withdraw(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private SessionUser seller(HttpSession session) { return sessions.requireRole(session, Role.SELLER); }

    private ArtifactSubmissionForm form(ApiDtos.ArtifactRequest request) {
        ArtifactSubmissionForm form = new ArtifactSubmissionForm();
        form.setTitle(request.title());
        form.setCategory(request.category());
        form.setEra(request.era());
        form.setStartingPrice(request.startingPrice());
        form.setClosesAt(request.closesAt());
        form.setDescription(request.description());
        return form;
    }
}
