package com.artifactalley.artifact;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ArtifactSubmissionForm {
    @NotBlank(message = "Please enter an artifact title.")
    @Size(max = 120, message = "Title must be at most 120 characters.")
    private String title;

    @NotNull(message = "Please choose a category.")
    private Category category;

    @NotBlank(message = "Please enter an era or year.")
    @Size(max = 60, message = "Era must be at most 60 characters.")
    private String era;

    @NotNull(message = "Please enter a starting price.")
    @DecimalMin(value = "1.00", message = "Starting price must be at least ₹1.")
    @Digits(integer = 10, fraction = 2, message = "Use a valid amount with up to two decimal places.")
    private BigDecimal startingPrice;

    @NotNull(message = "Please choose an auction closing time.")
    @Future(message = "The closing time must be in the future.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime closesAt;

    @NotBlank(message = "Please describe the artifact.")
    @Size(max = 1000, message = "Description must be at most 1,000 characters.")
    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getEra() { return era; }
    public void setEra(String era) { this.era = era; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalDateTime closesAt) { this.closesAt = closesAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
