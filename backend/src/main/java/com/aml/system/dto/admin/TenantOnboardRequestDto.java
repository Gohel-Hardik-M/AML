package com.aml.system.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantOnboardRequestDto {
    @NotBlank(message = "Tenant code is required")
    @Size(max = 64, message = "Tenant code must not exceed 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Tenant code may contain only letters, numbers, underscores, and hyphens")
    private String tenantCode;

    @NotBlank(message = "Bank name is required")
    @Size(max = 255, message = "Bank name must not exceed 255 characters")
    private String bankName;

    @NotBlank(message = "Admin username is required")
    @Size(max = 64, message = "Admin username must not exceed 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Admin username contains invalid characters")
    private String adminUsername;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Admin email must not exceed 254 characters")
    @Pattern(
            regexp = "^(?!.*\\.\\.)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$",
            message = "Admin email must contain a valid domain and top-level domain"
    )
    private String adminEmail; // <--- NEW VALIDATED FIELD
}