package com.aml.system.service;

import com.aml.system.dto.admin.RuleConfigUpdateDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.TenantRuleConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Bank Admin to view and update rule configurations (e.g. threshold values,
 * window minutes, enable/disable) for their specific tenant.
 */
@Slf4j
@Service
public class RuleConfigService {

    private final TenantRuleConfigRepository tenantRuleConfigRepository;
    private final JdbcTemplate masterJdbcTemplate;

    public RuleConfigService(TenantRuleConfigRepository tenantRuleConfigRepository,
                             @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
        this.tenantRuleConfigRepository = tenantRuleConfigRepository;
        this.masterJdbcTemplate = masterJdbcTemplate;
    }

    /**
     * Lists all rule configurations for the current bank tenant.
     */
    @Transactional(readOnly = true)
    public List<TenantRuleConfig> getTenantRules() {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context is missing.", HttpStatus.BAD_REQUEST);
        }
        return tenantRuleConfigRepository.findByTenantId(tenantId).stream()
            .filter(config -> isAllocated(tenantId, config.getRuleCode()))
            .toList();
    }

    /**
     * Gets a specific rule configuration by rule code.
     */
    @Transactional(readOnly = true)
    public TenantRuleConfig getRuleByCode(String ruleCode) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context is missing.", HttpStatus.BAD_REQUEST);
        }
        String normalizedRuleCode = ruleCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!isAllocated(tenantId, normalizedRuleCode)) {
            throw new AmlBusinessException(
                "Rule '" + normalizedRuleCode + "' is not allocated to this bank. Please contact the System Administrator.",
                    HttpStatus.NOT_FOUND
            );
        }
        return tenantRuleConfigRepository.findByTenantIdAndRuleCode(tenantId, normalizedRuleCode)
                .orElseThrow(() -> new AmlBusinessException(
                "Rule '" + normalizedRuleCode + "' not found for this bank.",
                        HttpStatus.NOT_FOUND
                ));
    }

    /**
     * Updates rule configuration thresholds / settings for the current bank.
     * Only non-null fields provided in the DTO are updated.
     */
    @Transactional
    public TenantRuleConfig updateRuleConfig(String ruleCode, RuleConfigUpdateDto dto) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AmlBusinessException("Tenant context is missing.", HttpStatus.BAD_REQUEST);
        }
        String normalizedRuleCode = ruleCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!isAllocated(tenantId, normalizedRuleCode)) {
            throw new AmlBusinessException(
                "Rule '" + normalizedRuleCode + "' is not allocated to this bank. Please contact the System Administrator.",
                    HttpStatus.NOT_FOUND
            );
        }

        // Find existing rule. If not allocated to this tenant, reject!
        TenantRuleConfig config = tenantRuleConfigRepository
                .findByTenantIdAndRuleCode(tenantId, normalizedRuleCode)
                .orElseThrow(() -> new AmlBusinessException(
                        "Rule '" + ruleCode + "' is not allocated to this bank. Please contact the System Administrator.",
                        HttpStatus.FORBIDDEN
                ));

        if (dto.getThresholdAmount() != null) {
            config.setThresholdAmount(dto.getThresholdAmount());
        }
        if (dto.getWindowMinutes() != null) {
            config.setWindowMinutes(dto.getWindowMinutes());
        }
        if (dto.getMaxCount() != null) {
            config.setMaxCount(dto.getMaxCount());
        }
        if (dto.getIsEnabled() != null) {
            config.setIsEnabled(dto.getIsEnabled());
        }
        if (dto.getPercentageDeviation() != null) {
            config.setPercentageDeviation(dto.getPercentageDeviation());
        }
        if (dto.getCustomParametersJson() != null) {
            config.setCustomParametersJson(dto.getCustomParametersJson());
        }

        TenantRuleConfig updated = tenantRuleConfigRepository.save(config);
        log.info("Updated rule '{}' configuration for tenant '{}'", ruleCode, tenantId);

        return updated;
    }

    private boolean isAllocated(String tenantId, String ruleCode) {
        Integer count = masterJdbcTemplate.queryForObject(
                "SELECT count(*) FROM aml_tenant_rule_allocations WHERE tenant_id = ? AND rule_code = ?",
                Integer.class,
                tenantId,
                ruleCode
        );
        return count != null && count > 0;
    }
}
