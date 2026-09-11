package com.aml.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MasterLoginRequestDto {
    @NotBlank(message = "Username is required")
    @Size(max = 128, message = "Username must not exceed 128 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username contains invalid characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password must not exceed 128 characters")
    private String password;
}