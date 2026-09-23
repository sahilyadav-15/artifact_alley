package com.artifactalley.security;

import com.artifactalley.api.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnBean(CsrfTokenService.class)
public class SecurityRequestFilter extends OncePerRequestFilter {
    private final CsrfTokenService csrf;
    private final ObjectMapper json;
    private final XmlMapper xml = XmlMapper.builder().findAndAddModules().build();
    private final SecureRandom random = new SecureRandom();
    @Autowired(required = false)
    private SecurityAuditService audit;

    public SecurityRequestFilter(CsrfTokenService csrf, ObjectMapper json) { this.csrf = csrf; this.json = json; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        addHeaders(request, response);
        HttpSession session = request.getSession(true);
        String token = csrf.ensureToken(session);
        request.setAttribute(CsrfTokenService.REQUEST_ATTRIBUTE, token);
        try {
            if (unsafe(request) && csrfRequired(request, session)) {
                String supplied = request.getRequestURI().startsWith("/api/")
                        ? request.getHeader(CsrfTokenService.HEADER) : request.getParameter(CsrfTokenService.PARAMETER);
                if (!csrf.matches(session, supplied)) {
                    if (audit != null) audit.record("CSRF_REJECTED", null, null, "invalid_token");
                    reject(request, response, requestId);
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }

    private boolean unsafe(HttpServletRequest request) {
        return !List.of("GET", "HEAD", "OPTIONS", "TRACE").contains(request.getMethod());
    }

    private boolean csrfRequired(HttpServletRequest request, HttpSession session) {
        return true;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String requestId) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if (request.getRequestURI().startsWith("/api/")) {
            ApiErrorResponse body = new ApiErrorResponse(OffsetDateTime.now(), 403, "CSRF_INVALID",
                    "A valid CSRF token is required.", request.getRequestURI(), requestId, List.of());
            if (acceptsXml(request)) {
                response.setContentType(MediaType.APPLICATION_XML_VALUE);
                xml.writeValue(response.getOutputStream(), body);
            } else {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                json.writeValue(response.getOutputStream(), body);
            }
            return;
        }
        request.setAttribute("securityMessage", "Your form expired or could not be verified. Return to the page and try again.");
        request.getRequestDispatcher("/WEB-INF/jsp/access-denied.jsp").forward(request, response);
    }

    private boolean acceptsXml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_XML_VALUE);
    }

    private void addHeaders(HttpServletRequest request, HttpServletResponse response) {
        byte[] nonceBytes = new byte[18];
        random.nextBytes(nonceBytes);
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        request.setAttribute("cspNonce", nonce);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        boolean h2Console = request.getRequestURI().startsWith("/h2-console");
        response.setHeader("X-Frame-Options", h2Console ? "SAMEORIGIN" : "DENY");
        response.setHeader("Content-Security-Policy", h2Console
                ? "default-src 'self'; frame-ancestors 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"
                : "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; "
                + "img-src 'self' data:; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                + "script-src 'self' 'nonce-" + nonce + "'");
        if (request.getRequestURI().startsWith("/account/") || request.getRequestURI().startsWith("/seller/")
                || request.getRequestURI().startsWith("/admin/") || request.getRequestURI().startsWith("/login")
                || request.getRequestURI().startsWith("/register") || request.getRequestURI().startsWith("/api/v1/session")
                || request.getRequestURI().startsWith("/api/v1/seller") || request.getRequestURI().startsWith("/api/v1/admin")) {
            response.setHeader("Cache-Control", "no-store");
        }
    }
}
