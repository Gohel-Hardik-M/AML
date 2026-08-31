package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.auth.AuthRequestDto;
import com.aml.system.dto.auth.AuthResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@RequestBody AuthRequestDto request) {
        log.info("User login attempt for username: [{}] on Tenant: [{}]", request.getUsername(), request.getTenantId());

        // Simulated JWT issuance for MVP v1
        AuthResponseDto authResponse = AuthResponseDto.builder()
                .token("jwt-mock-token-" + UUID.randomUUID())
                .tokenType("Bearer")
                .tenantId(request.getTenantId())
                .username(request.getUsername())
                .roles(List.of("ROLE_COMPLIANCE_OFFICER", "ROLE_ANALYST"))
                .expiresIn(86400)
                .build();

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Authentication successful"));
    }
}
