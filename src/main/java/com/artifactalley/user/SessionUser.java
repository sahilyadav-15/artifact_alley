package com.artifactalley.user;

import java.io.Serializable;

/** A small, password-free value stored under the signedInUser HTTP-session attribute. */
public final class SessionUser implements Serializable {
    private final Long id;
    private final String name;
    private final String email;
    private final Role role;

    private SessionUser(Long id, String name, String email, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public static SessionUser from(User user) {
        return new SessionUser(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
}
