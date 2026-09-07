package com.aml.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetDto {
    @NotBlank(message = "Tenant ID is required")
    @Size(max = 64, message = "Tenant ID must not exceed 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Tenant ID contains invalid characters")
    private String tenantId;

    @NotBlank(message = "Username is required")
    @Size(max = 128, message = "Username must not exceed 128 characters")
    private String username;

    @NotBlank(message = "Current password is required")
    @Size(max = 128, message = "Current password must not exceed 128 characters")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(max = 128, message = "New password must not exceed 128 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{10,}$",
            message = "Password must be at least 10 characters long, contain uppercase, lowercase, numbers, and special characters."
    )
    private String newPassword;
}