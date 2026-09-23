package com.artifactalley.user;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private final UserRepository userRepository;
    private final String initialAdminPassword;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public UserService(UserRepository userRepository,
                       @Value("${artifactalley.initial-admin-password}") String initialAdminPassword) {
        this.userRepository = userRepository;
        this.initialAdminPassword = initialAdminPassword;
    }

    public User register(RegistrationForm form) {
        String email = normaliseEmail(form.getEmail());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        if (form.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be created through registration.");
        }
        User user = new User(form.getName().trim(), email, hashPassword(form.getPassword()), form.getRole());
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String password) {
        return userRepository.findByEmailIgnoreCase(normaliseEmail(email))
                .filter(user -> passwordMatches(password, user.getPasswordHash()));
    }

    /** Creates the initial administrator only when absent; it never replaces an existing password. */
    public void ensureInitialAdmin() {
        String email = "admin@artifact.com";
        if (!userRepository.existsByRole(Role.ADMIN)) {
            String lowered = initialAdminPassword.toLowerCase(java.util.Locale.ROOT);
            if (activeProfiles != null && activeProfiles.contains("prod")
                    && (initialAdminPassword.length() < 12 || "123456".equals(initialAdminPassword)
                    || lowered.contains("password") || lowered.contains("admin"))) {
                throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must be a non-default value in production.");
            }
            userRepository.save(new User("Administrator", email, hashPassword(initialAdminPassword), Role.ADMIN));
        }
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        return userRepository.findById(userId)
                .filter(user -> passwordMatches(currentPassword, user.getPasswordHash()))
                .map(user -> {
                    user.changePasswordHash(hashPassword(newPassword));
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    /** The only self-service role change: a bidder may become a seller, never an administrator. */
    public Optional<User> becomeSeller(Long userId) {
        return userRepository.findById(userId)
                .filter(User::becomeSeller)
                .map(userRepository::save);
    }

    private String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] hash = deriveKey(password.toCharArray(), salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private boolean passwordMatches(String password, String storedValue) {
        try {
            String[] parts = storedValue.split(":", -1);
            if (parts.length != 3) return false;
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = deriveKey(password.toCharArray(), salt, iterations);
            return java.security.MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] deriveKey(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        }
    }
}
