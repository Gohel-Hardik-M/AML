package com.aml.system.controller;

import com.aml.system.dto.auth.LoginRequestDto;
import com.aml.system.dto.auth.LoginResponseDto;
import com.aml.system.dto.auth.MasterLoginRequestDto;
import com.aml.system.dto.auth.PasswordResetDto;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.service.AuthService;
import com.aml.system.service.MasterAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MasterAuthService masterAuthService;

    // Injected both services here
    public AuthController(AuthService authService, MasterAuthService masterAuthService) {
        this.authService = authService;
        this.masterAuthService = masterAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        try {
            // 1. Set the database context BEFORE the transactional service is called
            TenantContextHolder.setTenantId(request.getTenantId());

            // 2. Call the service
            LoginResponseDto response = authService.login(request, httpRequest);
            return ResponseEntity.ok(response);

        } finally {
            // 3. Always clean up!
            TenantContextHolder.clear();
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody PasswordResetDto request,
            HttpServletRequest httpRequest
    ) {
        try {
            // 1. Set context before transaction
            TenantContextHolder.setTenantId(request.getTenantId());

            // 2. Call the service
            authService.resetPassword(request, httpRequest);
            return ResponseEntity.ok("Password updated successfully. You can now log in.");

        } finally {
            TenantContextHolder.clear();
        }
    }

    // --- NEW ENDPOINT FOR GLOBAL SAAS ADMIN ---
    @PostMapping("/master/login")
    public ResponseEntity<LoginResponseDto> masterLogin(
            @Valid @RequestBody MasterLoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        LoginResponseDto response = masterAuthService.masterLogin(request, httpRequest);
        return ResponseEntity.ok(response);
    }
}