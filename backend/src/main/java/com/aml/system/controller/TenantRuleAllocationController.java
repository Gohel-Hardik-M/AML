package com.aml.system.controller;

import com.aml.system.dto.admin.TenantRuleAllocationDto;
import com.aml.system.service.TenantRuleAllocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/master/rules")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class TenantRuleAllocationController {

    private final TenantRuleAllocationService allocationService;

    public TenantRuleAllocationController(TenantRuleAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<Map<String, Object>>> getGlobalCatalog() {
        List<Map<String, Object>> catalog = allocationService.getGlobalCatalog();
        return ResponseEntity.ok(catalog);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<Map<String, Object>>> getAllocatedRules(@PathVariable String tenantId) {
        List<Map<String, Object>> allocated = allocationService.getAllocatedRules(tenantId);
        return ResponseEntity.ok(allocated);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllTenantAllocations() {
        return ResponseEntity.ok(allocationService.getAllTenantAllocations());
    }

    @PostMapping("/allocate")
    public ResponseEntity<Map<String, String>> allocateRules(@Valid @RequestBody TenantRuleAllocationDto dto) {
        String result = allocationService.allocateRulesToTenant(dto);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @DeleteMapping("/tenant/{tenantId}/{ruleCode}")
    public ResponseEntity<Map<String, String>> deallocateRule(
            @PathVariable String tenantId,
            @PathVariable String ruleCode
    ) {
        String result = allocationService.deallocateRule(tenantId, ruleCode);
        return ResponseEntity.ok(Map.of("message", result));
    }
}
