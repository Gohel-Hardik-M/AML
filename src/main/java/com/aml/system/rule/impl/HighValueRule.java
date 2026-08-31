package com.aml.system.rule.impl;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.rule.AmlRuleEvaluator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class HighValueRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_003_HIGH_VALUE";
    public static final String RULE_NAME = "High Value Single Transaction Threshold Exceeded";

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public Optional<Alert> evaluate(Transaction txn, TenantRuleConfig config, List<Transaction> history) {
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            return Optional.empty();
        }

        BigDecimal threshold = config.getThresholdAmount() != null ? config.getThresholdAmount() : BigDecimal.valueOf(50000.00);

        if (txn.getAmount() != null && txn.getAmount().compareTo(threshold) >= 0) {
            String narrative = String.format("High value transaction detected for customer %s: $%s (Threshold: $%s)",
                    txn.getCustomerId(), txn.getAmount().toPlainString(), threshold.toPlainString());

            Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID())
                    .tenantId(txn.getTenantId())
                    .transactionId(txn.getTransactionId())
                    .customerId(txn.getCustomerId())
                    .ruleCode(RULE_CODE)
                    .ruleName(RULE_NAME)
                    .severity(AlertSeverity.MEDIUM)
                    .triggeredAmount(txn.getAmount())
                    .narrative(narrative)
                    .detectionMetadataJson(String.format("{\"amount\":%s,\"threshold\":%s}", txn.getAmount(), threshold))
                    .isReviewed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            return Optional.of(alert);
        }

        return Optional.empty();
    }
}
