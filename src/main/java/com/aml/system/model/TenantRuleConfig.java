package com.aml.system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Tenant-Specific AML Rule Threshold Configuration.
 * Allows each tenant institution to customize detection limits per jurisdiction and risk appetite.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aml_tenant_rule_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_rule", columnNames = {"tenantId", "ruleCode"})
})
public class TenantRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID configId;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String ruleCode;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEnabled = true;

    // Standard parameterized thresholds for rules
    @Column(precision = 19, scale = 4)
    private BigDecimal thresholdAmount;

    @Column
    private Integer windowMinutes;

    @Column
    private Integer maxCount;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentageDeviation;

    @Column(columnDefinition = "TEXT")
    private String customParametersJson;
}
