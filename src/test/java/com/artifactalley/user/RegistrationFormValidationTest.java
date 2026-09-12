package com.artifactalley.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationFormValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsSixCharacterPasswordsButRejectsShorterOnes() {
        RegistrationForm form = new RegistrationForm();
        form.setName("Mira Rao");
        form.setEmail("mira@example.com");
        form.setRole(Role.BIDDER);
        form.setPassword("12345");
        form.setConfirmPassword("12345");

        assertTrue(validator.validate(form).stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));

        form.setPassword("123456");
        form.setConfirmPassword("123456");
        assertFalse(validator.validate(form).stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));
    }
}
