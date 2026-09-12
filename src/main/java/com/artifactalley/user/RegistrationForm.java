package com.artifactalley.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @NotBlank(message = "Please enter your name.")
    @Size(max = 60, message = "Name must be at most 60 characters.")
    private String name;

    @NotBlank(message = "Please enter an email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email must be at most 254 characters.")
    private String email;

    @NotBlank(message = "Please choose a password.")
    @Size(min = 6, max = 72, message = "Password must be 6 to 72 characters.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    @NotNull(message = "Please choose an account type.")
    private Role role = Role.BIDDER;

    @AssertTrue(message = "Passwords do not match.")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
