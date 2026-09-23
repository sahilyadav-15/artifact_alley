package com.artifactalley.artifact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ArtifactReviewForm {
    @NotBlank(message = "Please explain why this listing needs changes.")
    @Size(max = 500, message = "The rejection reason must be at most 500 characters.")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
