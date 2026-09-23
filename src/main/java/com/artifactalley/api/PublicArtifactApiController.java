package com.artifactalley.api;

import com.artifactalley.artifact.ArtifactSearchRequest;
import com.artifactalley.bid.Bid;
import com.artifactalley.bid.BiddingService;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/artifacts", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Validated
@Tag(name = "Public artifacts", description = "Privacy-safe auction catalogue and bidding")
public class PublicArtifactApiController {
    private final ArtifactApiReadService readService;
    private final BiddingService biddingService;
    private final ApiSessionUserResolver sessions;

    public PublicArtifactApiController(ArtifactApiReadService readService, BiddingService biddingService,
                                       ApiSessionUserResolver sessions) {
        this.readService = readService;
        this.biddingService = biddingService;
        this.sessions = sessions;
    }

    @GetMapping
    @Operation(summary = "List public artifacts")
    public ApiDtos.ArtifactPage list(@RequestParam(name = "q", required = false) String query,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String era,
                                     @RequestParam(required = false) String minPrice,
                                     @RequestParam(required = false) String maxPrice,
                                     @RequestParam(required = false) String endingWithin,
                                     @RequestParam(required = false) String sort,
                                     @RequestParam(required = false) String page,
                                     @RequestParam(required = false) String size) {
        return readService.publicPage(new ArtifactSearchRequest(query, category, era, minPrice, maxPrice,
                endingWithin, sort, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one public artifact")
    public ApiDtos.ArtifactView details(@PathVariable Long id) { return readService.publicArtifact(id); }

    @GetMapping("/{id}/bids")
    @Operation(summary = "Get privacy-safe bid history")
    public ApiDtos.BidHistory bids(@PathVariable Long id) { return readService.publicBids(id); }

    @PostMapping(value = "/{id}/bids", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Place a bid", description = "Requires a BIDDER session cookie")
    public ResponseEntity<ApiDtos.BidCreated> bid(@PathVariable Long id, @Valid @RequestBody ApiDtos.BidRequest request,
                                                   HttpSession session) {
        SessionUser user = sessions.requireRole(session, Role.BIDDER);
        Bid bid = biddingService.placeBid(id, user.getId(), request.amount());
        return ResponseEntity.status(201).header("Location", "/api/v1/artifacts/" + id + "/bids")
                .body(new ApiDtos.BidCreated(bid.getId(), id, bid.getAmount(), bid.getPlacedAt(),
                        bid.getArtifact().getCurrentPrice()));
    }
}
