package com.artifactalley.api;

import com.artifactalley.artifact.ArtifactReviewService;
import com.artifactalley.report.ArtifactActivityJdbcRepository;
import com.artifactalley.report.AuctionReport;
import com.artifactalley.report.AuctionReportJdbcRepository;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/admin", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Administration")
@SecurityRequirement(name = "sessionCookie")
public class AdminArtifactApiController {
    private final ArtifactReviewService reviewService;
    private final ArtifactApiReadService readService;
    private final AuctionReportJdbcRepository reports;
    private final ArtifactActivityJdbcRepository activity;
    private final ApiSessionUserResolver sessions;

    public AdminArtifactApiController(ArtifactReviewService reviewService, ArtifactApiReadService readService,
                                      AuctionReportJdbcRepository reports, ArtifactActivityJdbcRepository activity,
                                      ApiSessionUserResolver sessions) {
        this.reviewService = reviewService;
        this.readService = readService;
        this.reports = reports;
        this.activity = activity;
        this.sessions = sessions;
    }

    @GetMapping("/artifacts/pending")
    @Operation(summary = "List artifacts awaiting review")
    public ApiDtos.AdminArtifactList pending(HttpSession session) {
        admin(session);
        return new ApiDtos.AdminArtifactList(reviewService.pending().stream().map(readService::adminView).toList());
    }

    @PostMapping("/artifacts/{id}/approve")
    @Operation(summary = "Approve a pending artifact")
    public ApiDtos.AdminArtifactView approve(@PathVariable Long id, HttpSession session) {
        SessionUser user = admin(session);
        reviewService.approve(id, user.getId());
        return readService.adminView(id);
    }

    @PostMapping(value = "/artifacts/{id}/reject", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Reject a pending artifact with a reason")
    public ApiDtos.AdminArtifactView reject(@PathVariable Long id,
                                             @Valid @RequestBody ApiDtos.RejectionRequest request,
                                             HttpSession session) {
        SessionUser user = admin(session);
        reviewService.reject(id, user.getId(), request.reason());
        return readService.adminView(id);
    }

    @GetMapping("/reports/auction-summary")
    @Operation(summary = "Read the Spring JDBC auction summary")
    public AuctionReport summary(@RequestParam(defaultValue = "5") @Min(1) @Max(25) int limit,
                                 HttpSession session) {
        admin(session);
        return reports.summary(limit);
    }

    @GetMapping("/reports/artifact-activity")
    @Operation(summary = "Read the direct JDBC date-range demonstration")
    public List<ArtifactActivityJdbcRepository.ArtifactActivity> activity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpSession session) {
        admin(session);
        return activity.findBetween(from, to);
    }

    private SessionUser admin(HttpSession session) { return sessions.requireRole(session, Role.ADMIN); }
}
