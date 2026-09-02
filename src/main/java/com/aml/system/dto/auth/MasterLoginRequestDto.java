package com.aml.system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MasterLoginRequestDto {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}