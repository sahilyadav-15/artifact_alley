package com.artifactalley.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordForm {
    @NotBlank(message = "Please enter your current password.")
    @Size(max = 72, message = "Password must be at most 72 characters.")
    private String currentPassword;

    @NotBlank(message = "Please choose a new password.")
    @Size(min = 6, max = 72, message = "New password must be 6 to 72 characters.")
    private String newPassword;

    @NotBlank(message = "Please confirm your new password.")
    private String confirmPassword;

    @AssertTrue(message = "New passwords do not match.")
    public boolean isNewPasswordsMatch() { return newPassword != null && newPassword.equals(confirmPassword); }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
