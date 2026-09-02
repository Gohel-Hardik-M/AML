package com.aml.system.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantOnboardRequestDto {
    @NotBlank(message = "Tenant code is required")
    private String tenantCode;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Admin username is required")
    private String adminUsername;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid email format")
    private String adminEmail; // <--- NEW VALIDATED FIELD
}