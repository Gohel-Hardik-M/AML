package com.aml.system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA entity for the aml_tenant_rules table.
 * Each row represents a rule configuration for a specific tenant.
 *
 * Example: Tenant "HDFC" might have STRUCTURING_001 with threshold = 15000
 * while Tenant "SBI" has STRUCTURING_001 with threshold = 10000.
 */
@Entity
@Table(name = "aml_tenant_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id")
    private UUID configId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "threshold_amount", precision = 19, scale = 4)
    private BigDecimal thresholdAmount;

    @Column(name = "window_minutes")
    private Integer windowMinutes;

    @Column(name = "max_count")
    private Integer maxCount;

    @Column(name = "percentage_deviation", precision = 5, scale = 2)
    private BigDecimal percentageDeviation;

    @Column(name = "custom_parameters_json", columnDefinition = "TEXT")
    private String customParametersJson;
}
