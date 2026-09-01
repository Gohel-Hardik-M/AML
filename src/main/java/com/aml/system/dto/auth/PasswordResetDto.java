package com.aml.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordResetDto {
    @NotBlank
    private String tenantId;

    @NotBlank
    private String username;

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{10,}$",
            message = "Password must be at least 10 characters long, contain uppercase, lowercase, numbers, and special characters."
    )
    private String newPassword;
}