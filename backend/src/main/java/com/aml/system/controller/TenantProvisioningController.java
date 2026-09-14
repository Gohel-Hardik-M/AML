package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.admin.TenantOnboardRequestDto;
import com.aml.system.dto.admin.TenantSummaryDto;
import com.aml.system.service.TenantProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master/tenants")
public class TenantProvisioningController {

    private final TenantProvisioningService tenantProvisioningService;

    public TenantProvisioningController(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> onboardBank(@Valid @RequestBody TenantOnboardRequestDto request) {

        String resultMessage = tenantProvisioningService.onboardNewBank(request);

        return ResponseEntity.ok(ApiResponse.success(resultMessage));
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<TenantSummaryDto>>> listTenants() {
        return ResponseEntity.ok(ApiResponse.success(tenantProvisioningService.listTenants()));
    }
}