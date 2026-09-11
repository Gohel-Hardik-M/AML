package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.admin.TenantRuleAllocationDto;
import com.aml.system.service.TenantRuleAllocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for System Admin to allocate and manage AML rules for specific bank tenants.
 */
@RestController
@RequestMapping("/api/v1/master/rules")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class TenantRuleAllocationController {

    private final TenantRuleAllocationService allocationService;

    public TenantRuleAllocationController(TenantRuleAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    /**
     * View all available rules from the global catalog.
     */
    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGlobalCatalog() {
        List<Map<String, Object>> catalog = allocationService.getGlobalCatalog();
        return ResponseEntity.ok(ApiResponse.success(catalog));
    }

    /**
     * View which rules are currently allocated to a specific tenant.
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllocatedRules(@PathVariable String tenantId) {
        List<Map<String, Object>> allocated = allocationService.getAllocatedRules(tenantId);
        return ResponseEntity.ok(ApiResponse.success(allocated));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllTenantAllocations() {
        return ResponseEntity.ok(ApiResponse.success(allocationService.getAllTenantAllocations()));
    }

    /**
     * Allocate rules to a specific tenant.
     */
    @PostMapping("/allocate")
    public ResponseEntity<ApiResponse<Void>> allocateRules(@Valid @RequestBody TenantRuleAllocationDto dto) {
        String result = allocationService.allocateRulesToTenant(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Remove a rule allocation from a tenant.
     */
    @DeleteMapping("/tenant/{tenantId}/{ruleCode}")
    public ResponseEntity<ApiResponse<Void>> deallocateRule(
            @PathVariable String tenantId,
            @PathVariable String ruleCode
    ) {
        String result = allocationService.deallocateRule(tenantId, ruleCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
