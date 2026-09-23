package com.artifactalley.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import com.artifactalley.user.UserRepository;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationValidatorTest {
    @Test
    void acceptsCompleteSafeConfiguration() {
        assertDoesNotThrow(() -> validator(validEnvironment()).run(new DefaultApplicationArguments()));
    }

    @Test
    void rejectsLocalStorageAndInsecureOrigin() {
        MockEnvironment localStorage = validEnvironment().withProperty("artifactalley.images.storage", "local");
        assertThrows(IllegalStateException.class, () -> validator(localStorage).run(new DefaultApplicationArguments()));

        MockEnvironment httpOrigin = validEnvironment().withProperty("artifactalley.base-url", "http://example.test");
        assertThrows(IllegalStateException.class, () -> validator(httpOrigin).run(new DefaultApplicationArguments()));
    }

    @Test
    void requiresStrongBootstrapPasswordOnlyWhenNoAdministratorExists() {
        MockEnvironment environment = validEnvironment().withProperty("artifactalley.initial-admin-password", "123456");
        UserRepository users = mock(UserRepository.class);
        when(users.existsByRole(any())).thenReturn(false);
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment, users);
        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));

        environment.setProperty("artifactalley.initial-admin-password", "long-random-value-7842");
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    private ProductionConfigurationValidator validator(MockEnvironment environment) {
        UserRepository users = mock(UserRepository.class);
        when(users.existsByRole(any())).thenReturn(true);
        return new ProductionConfigurationValidator(environment, users);
    }

    private MockEnvironment validEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.flyway.enabled", "true")
                .withProperty("spring.flyway.baseline-on-migrate", "false")
                .withProperty("server.servlet.session.cookie.secure", "true")
                .withProperty("server.forward-headers-strategy", "framework")
                .withProperty("artifactalley.images.storage", "cloudinary")
                .withProperty("artifactalley.cloudinary.cloud-name", "cloud")
                .withProperty("artifactalley.cloudinary.api-key", "key")
                .withProperty("artifactalley.cloudinary.api-secret", "secret")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db.example/test")
                .withProperty("spring.datasource.username", "user")
                .withProperty("spring.datasource.password", "secret")
                .withProperty("spring.servlet.multipart.max-file-size", "5MB")
                .withProperty("spring.servlet.multipart.max-request-size", "26MB")
                .withProperty("artifactalley.base-url", "https://example.test");
        environment.setActiveProfiles("prod");
        return environment;
    }
}
