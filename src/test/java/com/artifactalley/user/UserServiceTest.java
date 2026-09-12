package com.artifactalley.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, "local-test-password");
    }

    @Test
    void registersNormalisedEmailWithHashedPassword() {
        RegistrationForm form = registration("  Mira Rao ", " MIRA@EXAMPLE.COM ", Role.SELLER);
        when(userRepository.findByEmailIgnoreCase("mira@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(form);

        assertEquals("Mira Rao", user.getName());
        assertEquals("mira@example.com", user.getEmail());
        assertEquals(Role.SELLER, user.getRole());
        assertNotEquals("correct horse battery staple", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("210000:"));
    }

    @Test
    void authenticatesOnlyWhenPasswordMatches() {
        RegistrationForm form = registration("Mira Rao", "mira@example.com", Role.BIDDER);
        when(userRepository.findByEmailIgnoreCase("mira@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User registered = userService.register(form);
        when(userRepository.findByEmailIgnoreCase("mira@example.com")).thenReturn(Optional.of(registered));

        assertTrue(userService.authenticate("MIRA@example.com", "correct horse battery staple").isPresent());
        assertTrue(userService.authenticate("mira@example.com", "wrong password").isEmpty());
    }

    @Test
    void rejectsAdminRegistrationAndDuplicateEmails() {
        RegistrationForm admin = registration("Admin", "admin@example.com", Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.register(admin));

        RegistrationForm duplicate = registration("Mira", "mira@example.com", Role.BIDDER);
        when(userRepository.findByEmailIgnoreCase("mira@example.com")).thenReturn(Optional.of(mock(User.class)));
        assertThrows(EmailAlreadyRegisteredException.class, () -> userService.register(duplicate));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsTheInitialAdminWithAHash() {
        when(userRepository.findByEmailIgnoreCase("admin@artifact.com")).thenReturn(Optional.empty());

        userService.ensureInitialAdmin();

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("admin@artifact.com", savedUser.getValue().getEmail());
        assertEquals(Role.ADMIN, savedUser.getValue().getRole());
        assertNotEquals("local-test-password", savedUser.getValue().getPasswordHash());
    }

    @Test
    void changesPasswordOnlyAfterTheCurrentPasswordMatches() {
        RegistrationForm form = registration("Mira", "mira@example.com", Role.BIDDER);
        when(userRepository.findByEmailIgnoreCase("mira@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = userService.register(form);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("mira@example.com")).thenReturn(Optional.of(user));

        assertFalse(userService.changePassword(5L, "wrong password", "new password 123"));
        assertTrue(userService.changePassword(5L, "correct horse battery staple", "new password 123"));
        assertTrue(userService.authenticate("mira@example.com", "new password 123").isPresent());
        assertTrue(userService.authenticate("mira@example.com", "correct horse battery staple").isEmpty());
    }

    @Test
    void promotesOnlyBiddersToSellers() {
        User bidder = new User("Mira", "mira@example.com", "hash", Role.BIDDER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(bidder));
        when(userRepository.save(bidder)).thenReturn(bidder);

        assertEquals(Role.SELLER, userService.becomeSeller(7L).orElseThrow().getRole());

        User admin = new User("Admin", "admin@example.com", "hash", Role.ADMIN);
        when(userRepository.findById(8L)).thenReturn(Optional.of(admin));
        assertTrue(userService.becomeSeller(8L).isEmpty());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    private RegistrationForm registration(String name, String email, Role role) {
        RegistrationForm form = new RegistrationForm();
        form.setName(name);
        form.setEmail(email);
        form.setPassword("correct horse battery staple");
        form.setConfirmPassword("correct horse battery staple");
        form.setRole(role);
        return form;
    }
}
