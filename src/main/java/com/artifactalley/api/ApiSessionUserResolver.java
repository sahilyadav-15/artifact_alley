package com.artifactalley.api;

import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import com.artifactalley.security.SessionAuthentication;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class ApiSessionUserResolver {
    private final SessionAuthentication authentication;

    public ApiSessionUserResolver(SessionAuthentication authentication) { this.authentication = authentication; }

    public SessionUser requireRole(HttpSession session, Role role) {
        var user = authentication.currentUser(session).orElseThrow(ApiAccessException::authenticationRequired);
        if (user.getRole() != role) throw ApiAccessException.forbidden();
        return SessionUser.from(user);
    }
}
