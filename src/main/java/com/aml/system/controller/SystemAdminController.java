package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.TenantRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TENANT_ADMIN')") // <--- Only Tenant Admins can touch these rules
public class SystemAdminController {

    private final TenantRuleConfigRepository tenantRuleConfigRepository;

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<TenantRuleConfig>>> getTenantRules() {
        String tenantId = TenantContextHolder.getTenantId();
        List<TenantRuleConfig> configs = tenantRuleConfigRepository.findByTenantIdAndIsEnabledTrue(tenantId);
        return ResponseEntity.ok(ApiResponse.success(configs, "Retrieved tenant rule configurations"));
    }

    @PutMapping("/rules/{ruleCode}")
    public ResponseEntity<ApiResponse<TenantRuleConfig>> updateRuleConfig(
            @PathVariable String ruleCode,
            @RequestBody TenantRuleConfig updatedConfig
    ) {
        String tenantId = TenantContextHolder.getTenantId();
        TenantRuleConfig config = tenantRuleConfigRepository.findByTenantIdAndRuleCode(tenantId, ruleCode)
                .orElseGet(() -> TenantRuleConfig.builder()
                        .tenantId(tenantId)
                        .ruleCode(ruleCode)
                        .build());

        config.setIsEnabled(updatedConfig.getIsEnabled());
        config.setThresholdAmount(updatedConfig.getThresholdAmount());
        config.setWindowMinutes(updatedConfig.getWindowMinutes());
        config.setMaxCount(updatedConfig.getMaxCount());
        config.setPercentageDeviation(updatedConfig.getPercentageDeviation());

        TenantRuleConfig saved = tenantRuleConfigRepository.save(config);
        log.info("Updated rule config [{}] for tenant [{}]", ruleCode, tenantId);
        return ResponseEntity.ok(ApiResponse.success(saved, "Rule configuration updated successfully"));
    }
}
