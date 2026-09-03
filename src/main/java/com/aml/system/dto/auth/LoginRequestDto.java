package com.aml.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    @NotBlank(message = "Username is required")
    @Size(max = 64, message = "Username must not exceed 64 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password must not exceed 128 characters")
    private String password;

    @NotBlank(message = "Tenant ID is required")
    @Size(max = 64, message = "Tenant ID must not exceed 64 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Tenant ID contains invalid characters")
    private String tenantId; // Specifies the tenant/bank database route (e.g., "HDFC")
}