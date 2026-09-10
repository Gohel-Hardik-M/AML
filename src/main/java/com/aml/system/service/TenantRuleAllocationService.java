package com.aml.system.service;

import com.aml.system.dto.admin.TenantRuleAllocationDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.TenantRuleConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for System Admin to allocate AML rules to specific bank tenants.
 * System Admin controls which rules each bank is allowed to use.
 */
@Slf4j
@Service
public class TenantRuleAllocationService {

    private final JdbcTemplate masterJdbcTemplate;
    private final TenantRuleConfigRepository tenantRuleConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TenantRuleAllocationService(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            TenantRuleConfigRepository tenantRuleConfigRepository
    ) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
        this.tenantRuleConfigRepository = tenantRuleConfigRepository;
    }

    /**
     * Lists all rules available in the global catalog with display names.
     */
    public List<Map<String, Object>> getGlobalCatalog() {
        String sql = "SELECT id, typology_name AS rule_code, typology_name, description, "
                + "default_thresholds::text AS default_thresholds FROM aml_global_rule_catalog ORDER BY typology_name";
        return masterJdbcTemplate.queryForList(sql).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            String ruleCode = (String) row.get("rule_code");
            String description = (String) row.get("description");
            item.put("ruleCode", ruleCode);
            item.put("ruleName", description != null ? description : getRuleDisplayName(ruleCode));
            item.put("description", description != null ? description : getRuleDisplayName(ruleCode));
            item.put("defaultThresholds", parseJson(row.get("default_thresholds")));
            return item;
        }).toList();
    }

    /**
     * Lists rules currently allocated to a specific tenant.
     */
    public List<Map<String, Object>> getAllocatedRules(String tenantId) {
        verifyTenantExists(tenantId);

        String sql = "SELECT id, tenant_id, rule_code, rule_name, allocated_at FROM aml_tenant_rule_allocations WHERE tenant_id = ? ORDER BY rule_code";
        return masterJdbcTemplate.queryForList(sql, tenantId);
    }

    /**
     * Allocates rules to a tenant in Master DB and initializes their configs in Tenant DB.
     * Strictly validates every rule code against aml_global_rule_catalog.
     */
    public String allocateRulesToTenant(TenantRuleAllocationDto dto) {
        String tenantId = dto.getTenantId().trim().toUpperCase(Locale.ROOT);
        verifyTenantExists(tenantId);

        if (dto.getRuleCodes() == null || dto.getRuleCodes().isEmpty()) {
            throw new AmlBusinessException("At least one rule code must be provided.", HttpStatus.BAD_REQUEST);
        }

        // 1. Fetch valid catalog rules from aml_global_rule_catalog
        String catalogSql = "SELECT typology_name, description, default_thresholds::text AS default_thresholds "
            + "FROM aml_global_rule_catalog ORDER BY typology_name";
        List<Map<String, Object>> catalogRows = masterJdbcTemplate.queryForList(catalogSql);
        Map<String, String> catalogMap = catalogRows.stream()
                .collect(Collectors.toMap(
                row -> ((String) row.get("typology_name")).trim().toUpperCase(Locale.ROOT),
                row -> String.valueOf(row.get("default_thresholds"))
                ));

        List<String> requestedCodes = dto.getRuleCodes().stream()
            .map(code -> code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
            .distinct()
            .toList();
        List<String> invalidCodes = requestedCodes.stream()
            .filter(code -> !catalogMap.containsKey(code))
                .toList();

        if (!invalidCodes.isEmpty()) {
            throw new AmlBusinessException(
                    "Invalid rule code(s): " + invalidCodes + ". Valid rule codes: " + catalogMap.keySet(),
                    HttpStatus.BAD_REQUEST
            );
        }

        // 3. Find currently allocated rules to avoid false duplicate counts
        String existingSql = "SELECT rule_code FROM aml_tenant_rule_allocations WHERE tenant_id = ?";
        Set<String> currentlyAllocated = new HashSet<>(masterJdbcTemplate.queryForList(existingSql, String.class, tenantId));

        List<String> newlyAllocated = new ArrayList<>();
        List<String> alreadyAllocated = new ArrayList<>();

        for (String ruleCode : requestedCodes) {
            if (currentlyAllocated.contains(ruleCode)) {
                syncTenantRuleConfig(tenantId, ruleCode, catalogMap.get(ruleCode));
                alreadyAllocated.add(ruleCode);
                continue;
            }

                String ruleName = catalogRows.stream()
                    .filter(row -> ruleCode.equalsIgnoreCase((String) row.get("typology_name")))
                    .map(row -> (String) row.get("description"))
                    .findFirst()
                    .filter(description -> !description.isBlank())
                    .orElseGet(() -> getRuleDisplayName(ruleCode));

            // Record in master allocation table
            String insertSql = """
                INSERT INTO aml_tenant_rule_allocations (tenant_id, rule_code, rule_name)
                VALUES (?, ?, ?)
                ON CONFLICT (tenant_id, rule_code) DO NOTHING
            """;
            masterJdbcTemplate.update(insertSql, tenantId, ruleCode, ruleName);

            // Initialize the rule in tenant DB with catalog default thresholds
            syncTenantRuleConfig(tenantId, ruleCode, catalogMap.get(ruleCode));

            newlyAllocated.add(ruleCode);
        }

        log.info("Rule allocation for tenant '{}': newly allocated={}, already allocated={}",
                tenantId, newlyAllocated, alreadyAllocated);

        if (newlyAllocated.isEmpty()) {
            return "No new rules allocated. All specified rule(s) " + alreadyAllocated + " are already allocated to tenant '" + tenantId + "'.";
        } else if (!alreadyAllocated.isEmpty()) {
            return "Successfully allocated " + newlyAllocated.size() + " new rule(s): " + newlyAllocated
                    + ". The following rule(s) were already allocated: " + alreadyAllocated + ".";
        } else {
            return "Successfully allocated " + newlyAllocated.size() + " rule(s) " + newlyAllocated + " to tenant '" + tenantId + "'.";
        }
    }

    /**
     * Removes a rule allocation from a tenant.
     */
    public String deallocateRule(String tenantId, String ruleCode) {
        String cleanTenantId = tenantId.trim().toUpperCase(Locale.ROOT);
        String cleanRuleCode = ruleCode.trim();
        verifyTenantExists(cleanTenantId);

        // Check if the allocation actually exists
        String checkSql = "SELECT count(*) FROM aml_tenant_rule_allocations WHERE tenant_id = ? AND rule_code = ?";
        Integer count = masterJdbcTemplate.queryForObject(checkSql, Integer.class, cleanTenantId, cleanRuleCode);
        if (count == null || count == 0) {
            throw new AmlBusinessException(
                    "Rule '" + cleanRuleCode + "' is not allocated to tenant '" + cleanTenantId + "'.",
                    HttpStatus.NOT_FOUND
            );
        }

        // 1. Remove from master allocations
        String deleteSql = "DELETE FROM aml_tenant_rule_allocations WHERE tenant_id = ? AND rule_code = ?";
        masterJdbcTemplate.update(deleteSql, cleanTenantId, cleanRuleCode);

        // 2. Disable the rule in the tenant database
        try {
            TenantContextHolder.setTenantId(cleanTenantId);
            tenantRuleConfigRepository.findByTenantIdAndRuleCode(cleanTenantId, cleanRuleCode).ifPresent(config -> {
                config.setIsEnabled(false);
                tenantRuleConfigRepository.save(config);
            });
        } catch (Exception e) {
            log.warn("Could not disable rule '{}' in tenant DB '{}': {}", cleanRuleCode, cleanTenantId, e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }

        return "Rule '" + cleanRuleCode + "' removed from tenant '" + cleanTenantId + "'.";
    }

    private void verifyTenantExists(String tenantId) {
        Integer count = masterJdbcTemplate.queryForObject(
            "SELECT count(*) FROM aml_tenant_registry WHERE tenant_id = ? AND is_active = TRUE",
                Integer.class,
                tenantId
        );
        if (count == null || count == 0) {
            throw new AmlBusinessException("Tenant '" + tenantId + "' does not exist in registry.", HttpStatus.NOT_FOUND);
        }
    }

    public String getRuleDisplayName(String ruleCode) {
        return switch (ruleCode) {
            case "STRUCTURING_001" -> "Structuring / Smurfing Cash Deposits";
            case "VELOCITY_001" -> "Rapid Transaction Velocity Check";
            case "CROSS_BORDER_001" -> "Cross-Border High Value Transfer";
            case "CRYPTO_001" -> "Cryptocurrency Transaction Pattern";
            case "GEO_RISK_001" -> "High-Risk Geographic Route";
            case "SMURFING_001" -> "Smurfing Layering Network";
            default -> ruleCode;
        };
    }

    private TenantRuleConfig buildDefaultConfig(String tenantId, String ruleCode, String defaultThresholdsJson) {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .tenantId(tenantId)
                .ruleCode(ruleCode)
                .isEnabled(true)
                .build();

        if (defaultThresholdsJson != null && !defaultThresholdsJson.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(defaultThresholdsJson);
                if (root.has("threshold_amount")) {
                    config.setThresholdAmount(new BigDecimal(root.get("threshold_amount").asText()));
                }
                if (root.has("window_minutes")) {
                    config.setWindowMinutes(root.get("window_minutes").asInt());
                }
                if (root.has("max_count")) {
                    config.setMaxCount(root.get("max_count").asInt());
                }
                if (root.has("percentage_deviation")) {
                    config.setPercentageDeviation(new BigDecimal(root.get("percentage_deviation").asText()));
                }
                if (root.has("custom_parameters_json")) {
                    config.setCustomParametersJson(root.get("custom_parameters_json").toString());
                }
            } catch (Exception e) {
                log.warn("Could not parse default thresholds for rule '{}': {}", ruleCode, e.getMessage());
            }
        }
        return config;
    }

    private void syncTenantRuleConfig(String tenantId, String ruleCode, String defaultThresholdsJson) {
        try {
            TenantContextHolder.setTenantId(tenantId);
            var existing = tenantRuleConfigRepository.findByTenantIdAndRuleCode(tenantId, ruleCode);
            if (existing.isEmpty()) {
                tenantRuleConfigRepository.save(buildDefaultConfig(tenantId, ruleCode, defaultThresholdsJson));
            } else {
                TenantRuleConfig config = existing.get();
                config.setIsEnabled(true);
                tenantRuleConfigRepository.save(config);
            }
        } catch (Exception e) {
            log.error("Could not sync rule '{}' to tenant DB '{}'. Allocation was not completed: {}",
                    ruleCode, tenantId, e.getMessage(), e);
            throw new AmlBusinessException(
                    "Rule '" + ruleCode + "' could not be initialized for tenant '" + tenantId + "'.",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    e
            );
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Object parseJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            return value.toString();
        }
    }
}
