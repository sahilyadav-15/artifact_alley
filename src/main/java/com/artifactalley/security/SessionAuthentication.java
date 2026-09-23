package com.artifactalley.security;

import com.artifactalley.user.AuthController;
import com.artifactalley.user.Role;
import com.artifactalley.user.SessionUser;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SessionAuthentication {
    private final UserRepository users;

    public SessionAuthentication(UserRepository users) { this.users = users; }

    public Optional<User> currentUser(HttpSession session) {
        if (session == null) return Optional.empty();
        Object value = session.getAttribute(AuthController.SIGNED_IN_USER);
        if (!(value instanceof SessionUser signedIn)) return Optional.empty();
        Optional<User> current = users.findById(signedIn.getId());
        if (current.isEmpty()) session.removeAttribute(AuthController.SIGNED_IN_USER);
        else session.setAttribute(AuthController.SIGNED_IN_USER, SessionUser.from(current.get()));
        return current;
    }

    public Optional<User> currentUserWithRole(HttpSession session, Role role) {
        return currentUser(session).filter(user -> user.getRole() == role);
    }
}
