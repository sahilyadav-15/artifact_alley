package com.artifactalley.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CsrfTokenService {
    public static final String SESSION_ATTRIBUTE = "csrfToken";
    public static final String REQUEST_ATTRIBUTE = "csrfToken";
    public static final String PARAMETER = "_csrf";
    public static final String HEADER = "X-CSRF-Token";
    private final SecureRandom random = new SecureRandom();

    public String ensureToken(HttpSession session) {
        Object existing = session.getAttribute(SESSION_ATTRIBUTE);
        if (existing instanceof String token && !token.isBlank()) return token;
        return rotateToken(session);
    }

    public String rotateToken(HttpSession session) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    public void clear(HttpSession session) { session.removeAttribute(SESSION_ATTRIBUTE); }

    public boolean matches(HttpSession session, String supplied) {
        Object expected = session.getAttribute(SESSION_ATTRIBUTE);
        if (!(expected instanceof String token) || supplied == null) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
