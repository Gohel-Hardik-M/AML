package com.aml.system.controller;

import com.aml.system.dto.auth.*;
import com.aml.system.exception.BadRequestException;
import com.aml.system.exception.UnauthorizedException;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.multitenancy.TenantRoutingDataSource;
import com.aml.system.service.AuthService;
import com.aml.system.service.MasterAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

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
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String tenantId = request.getTenantId() != null ? request.getTenantId().trim() : null;
        if (tenantId != null) {
            tenantId = tenantId.toUpperCase(java.util.Locale.ROOT);
            request.setTenantId(tenantId);
        }
        if (!routingDataSource.hasTenant(tenantId)) {
            throw new BadRequestException("Tenant '" + tenantId + "' does not exist or is not active.");
        }

        try {
            TenantContextHolder.setTenantId(tenantId);
            LoginResponseDto response = authService.login(request, httpRequest);
            return ResponseEntity.ok(response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @PostMapping({"/reset-password", "/change-password"})
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetDto request,
            HttpServletRequest httpRequest,
            Principal principal
    ) {
        String tenantId = TenantContextHolder.getTenantId();
        if (principal == null || tenantId == null || tenantId.isBlank()) {
            throw new UnauthorizedException("Authenticated user context is required.");
        }

        if ("MASTER".equalsIgnoreCase(tenantId)) {
            masterAuthService.resetMasterPassword(request, principal.getName(), httpRequest);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
        }

        if (!routingDataSource.hasTenant(tenantId)) {
            throw new BadRequestException("Authenticated tenant does not exist or is not active.");
        }

        try {
            TenantContextHolder.setTenantId(tenantId);
            authService.resetPassword(request, principal.getName(), tenantId, httpRequest);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully. You can now log in."));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @PostMapping("/master/login")
    public ResponseEntity<LoginResponseDto> masterLogin(
            @Valid @RequestBody MasterLoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        LoginResponseDto response = masterAuthService.masterLogin(request, httpRequest);
        return ResponseEntity.ok(response);
    }
}