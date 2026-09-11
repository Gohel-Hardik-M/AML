package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.auth.*;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.multitenancy.TenantRoutingDataSource;
import com.aml.system.service.AuthService;
import com.aml.system.service.MasterAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MasterAuthService masterAuthService;
    private final TenantRoutingDataSource routingDataSource;

    public AuthController(AuthService authService,
                          MasterAuthService masterAuthService,
                          TenantRoutingDataSource routingDataSource) {
        this.authService = authService;
        this.masterAuthService = masterAuthService;
        this.routingDataSource = routingDataSource;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String tenantId = request.getTenantId() != null ? request.getTenantId().trim() : null;
        if (tenantId != null) {
            tenantId = tenantId.toUpperCase(java.util.Locale.ROOT);
            request.setTenantId(tenantId);
        }
        if (!routingDataSource.hasTenant(tenantId)) {
            throw new AmlBusinessException("Tenant '" + tenantId + "' does not exist or is not active.", HttpStatus.BAD_REQUEST);
        }

        try {
            // 1. Set the database context BEFORE the transactional service is called
            TenantContextHolder.setTenantId(tenantId);

            // 2. Call the service
            LoginResponseDto response = authService.login(request, httpRequest);
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));

        } finally {
            // 3. Always clean up!
            TenantContextHolder.clear();
        }
    }

    /**
     * Password reset — NOW requires JWT authentication (audit finding #17).
     * The SecurityConfig only permits /login and /master/login without JWT.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetDto request,
            HttpServletRequest httpRequest
    ) {
        String tenantId = request.getTenantId() != null ? request.getTenantId().trim() : null;
        if (!routingDataSource.hasTenant(tenantId)) {
            throw new AmlBusinessException("Tenant '" + tenantId + "' does not exist or is not active.", HttpStatus.BAD_REQUEST);
        }

        try {
            // 1. Set context before transaction
            TenantContextHolder.setTenantId(tenantId);

            // 2. Call the service
            authService.resetPassword(request, httpRequest);
            return ResponseEntity.ok(ApiResponse.success("Password updated successfully. You can now log in."));

        } finally {
            TenantContextHolder.clear();
        }
    }

    // --- ENDPOINT FOR GLOBAL SAAS ADMIN ---
    @PostMapping("/master/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> masterLogin(
            @Valid @RequestBody MasterLoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        LoginResponseDto response = masterAuthService.masterLogin(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Master login successful"));
    }
}