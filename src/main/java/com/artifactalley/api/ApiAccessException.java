package com.artifactalley.api;

public final class ApiAccessException extends RuntimeException {
    private final boolean authenticated;

    private ApiAccessException(String message, boolean authenticated) {
        super(message);
        this.authenticated = authenticated;
    }

    public static ApiAccessException authenticationRequired() {
        return new ApiAccessException("An authenticated session is required.", false);
    }

    public static ApiAccessException forbidden() {
        return new ApiAccessException("Your account does not have the required role.", true);
    }

    public boolean isAuthenticated() { return authenticated; }
}
