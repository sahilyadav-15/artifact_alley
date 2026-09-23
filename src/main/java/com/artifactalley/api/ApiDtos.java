package com.artifactalley.api;

import com.artifactalley.artifact.ArtifactStatus;
import com.artifactalley.artifact.Category;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() { }

    public record ArtifactView(Long id, String title, Category category, String era,
                               BigDecimal startingPrice, BigDecimal currentPrice,
                               LocalDateTime closesAt, ArtifactStatus status, String description,
                               String coverImageUrl, long bidCount, BigDecimal minimumNextBid) { }

    @JacksonXmlRootElement(localName = "artifacts")
    public record ArtifactPage(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "artifact") List<ArtifactView> content,
            int page, int size, long totalElements, int totalPages, String sort,
            Map<String, String> appliedFilters) { }

    public record BidView(BigDecimal amount, LocalDateTime placedAt, String bidderDisplayName) { }

    @JacksonXmlRootElement(localName = "bidHistory")
    public record BidHistory(Long artifactId,
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "bid") List<BidView> bids, int count) { }

    public record BidRequest(
            @NotNull(message = "Amount is required.")
            @DecimalMin(value = "0.01", message = "Amount must be positive.")
            @Digits(integer = 10, fraction = 2, message = "Amount may have at most two decimal places.")
            BigDecimal amount) { }

    public record BidCreated(Long id, Long artifactId, BigDecimal amount, LocalDateTime placedAt,
                             BigDecimal currentPrice) { }

    public record ArtifactRequest(
            @NotBlank @Size(max = 120) String title,
            @NotNull Category category,
            @NotBlank @Size(max = 60) String era,
            @NotNull @DecimalMin("1.00") @Digits(integer = 10, fraction = 2) BigDecimal startingPrice,
            @NotNull @Future LocalDateTime closesAt,
            @NotBlank @Size(max = 1000) String description) { }

    public record SellerArtifactView(Long id, String title, Category category, String era,
                                     BigDecimal startingPrice, BigDecimal currentPrice,
                                     LocalDateTime closesAt, ArtifactStatus status, String description,
                                     LocalDateTime submittedAt, LocalDateTime updatedAt,
                                     String rejectionReason, long bidCount, List<String> imageUrls) { }

    @JacksonXmlRootElement(localName = "sellerArtifacts")
    public record SellerArtifactList(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "artifact") List<SellerArtifactView> artifacts) { }

    public record AdminArtifactView(Long id, String title, Category category, String era,
                                    BigDecimal startingPrice, LocalDateTime closesAt, ArtifactStatus status,
                                    String description, Long sellerId, String sellerName,
                                    LocalDateTime submittedAt, LocalDateTime reviewedAt, String rejectionReason) { }

    @JacksonXmlRootElement(localName = "pendingArtifacts")
    public record AdminArtifactList(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "artifact") List<AdminArtifactView> artifacts) { }

    public record RejectionRequest(@NotBlank @Size(max = 500) String reason) { }
}
