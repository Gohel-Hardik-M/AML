package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.admin.TenantOnboardRequestDto;
import com.aml.system.service.TenantProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master/tenants") // Notice the different path to separate from tenant-level admin
public class TenantProvisioningController {

    private final TenantProvisioningService tenantProvisioningService;

    public TenantProvisioningController(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    // Only global SYSTEM_ADMIN users can onboard brand new banks
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<String>> onboardBank(@Valid @RequestBody TenantOnboardRequestDto request) {

        String tempPassword = tenantProvisioningService.onboardNewBank(request);

        return ResponseEntity.ok(ApiResponse.success(
                "Temporary Password: " + tempPassword,
                "Tenant database provisioned, migrated, and initialized successfully."
        ));
    }
}