package com.artifactalley.bid;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AuctionExceptionHandler {
    @ExceptionHandler(ArtifactNotFoundException.class)
    public String artifactNotFound(ArtifactNotFoundException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("errorMessage", exception.getMessage());
        return "not-found";
    }
}
