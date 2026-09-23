package com.artifactalley.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import com.artifactalley.user.Role;
import com.artifactalley.user.UserRepository;

import java.net.URI;
import java.util.Arrays;

@Component
@Profile("prod")
@Order(0)
public class ProductionConfigurationValidator implements ApplicationRunner {
    private final Environment environment;
    private final UserRepository users;

    public ProductionConfigurationValidator(Environment environment, UserRepository users) {
        this.environment = environment;
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireExact("spring.jpa.hibernate.ddl-auto", "validate");
        requireExact("spring.flyway.enabled", "true");
        requireExact("spring.flyway.baseline-on-migrate", "false");
        requireExact("server.servlet.session.cookie.secure", "true");
        requireExact("server.forward-headers-strategy", "framework");
        requireExact("artifactalley.images.storage", "cloudinary");
        require("artifactalley.cloudinary.cloud-name");
        require("artifactalley.cloudinary.api-key");
        require("artifactalley.cloudinary.api-secret");
        require("spring.datasource.username");
        require("spring.datasource.password");
        String jdbc = require("spring.datasource.url");
        if (!jdbc.startsWith("jdbc:postgresql://")) fail("JDBC_DATABASE_URL must use PostgreSQL.");
        String baseUrl = require("artifactalley.base-url");
        URI uri;
        try { uri = URI.create(baseUrl); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("APP_BASE_URL must be a valid HTTPS URL."); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) fail("APP_BASE_URL must be a valid HTTPS URL.");
        requireExact("spring.servlet.multipart.max-file-size", "5MB");
        requireExact("spring.servlet.multipart.max-request-size", "26MB");
        if (!users.existsByRole(Role.ADMIN)) {
            String password = require("artifactalley.initial-admin-password");
            String lower = password.toLowerCase(java.util.Locale.ROOT);
            if (password.length() < 12 || lower.equals("123456") || lower.contains("password") || lower.contains("admin")) {
                fail("INITIAL_ADMIN_PASSWORD must be at least 12 characters and not a known development value while bootstrap is required.");
            }
        }
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) fail("The prod profile must be active.");
    }

    private String require(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) fail(key + " is required in production.");
        return value;
    }
    private void requireExact(String key, String expected) {
        if (!expected.equalsIgnoreCase(environment.getProperty(key, ""))) fail(key + " must be " + expected + " in production.");
    }
    private static void fail(String message) { throw new IllegalStateException("Unsafe production configuration: " + message); }
}
