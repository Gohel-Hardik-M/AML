package com.aml.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequestDto {
    @NotBlank(message = "Token is required for logout")
    private String token;
}
