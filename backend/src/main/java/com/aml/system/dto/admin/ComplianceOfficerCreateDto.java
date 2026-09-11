package com.aml.system.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating a new Compliance Officer.
 * Bank Admin sends this to add a new officer to their tenant.
 */
@Data
public class ComplianceOfficerCreateDto {

    @NotBlank(message = "Username is required")
    @Size(max = 64, message = "Username must not exceed 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username contains invalid characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 128, message = "Full name must not exceed 128 characters")
    private String fullName;
}
