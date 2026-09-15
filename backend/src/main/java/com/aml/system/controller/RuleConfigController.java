package com.aml.system.controller;

import com.aml.system.dto.admin.RuleConfigUpdateDto;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.service.RuleConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-admin/rules")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class RuleConfigController {

    private final RuleConfigService ruleConfigService;

    public RuleConfigController(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @GetMapping
    public ResponseEntity<List<TenantRuleConfig>> getTenantRules() {
        List<TenantRuleConfig> rules = ruleConfigService.getTenantRules();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{ruleCode}")
    public ResponseEntity<TenantRuleConfig> getRuleByCode(@PathVariable String ruleCode) {
        TenantRuleConfig config = ruleConfigService.getRuleByCode(ruleCode);
        return ResponseEntity.ok(config);
    }

    @PutMapping("/{ruleCode}")
    public ResponseEntity<TenantRuleConfig> updateRuleConfig(
            @PathVariable String ruleCode,
            @Valid @RequestBody RuleConfigUpdateDto dto
    ) {
        TenantRuleConfig updated = ruleConfigService.updateRuleConfig(ruleCode, dto);
        return ResponseEntity.ok(updated);
    }
}
