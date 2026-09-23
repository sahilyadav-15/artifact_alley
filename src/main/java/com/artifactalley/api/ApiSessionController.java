package com.artifactalley.api;

import com.artifactalley.security.CsrfTokenService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/session")
public class ApiSessionController {
    private final CsrfTokenService csrf;

    public ApiSessionController(CsrfTokenService csrf) { this.csrf = csrf; }

    @GetMapping(value = "/csrf", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Create or return the CSRF token for the current session",
            description = "Send token in X-CSRF-Token on authenticated POST, PUT, PATCH, and DELETE requests.")
    public CsrfToken csrf(HttpSession session) {
        return new CsrfToken(CsrfTokenService.HEADER, csrf.ensureToken(session));
    }

    public record CsrfToken(String headerName, String token) { }
}
