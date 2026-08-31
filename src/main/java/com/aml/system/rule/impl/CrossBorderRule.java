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
public class CrossBorderRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_005_CROSS_BORDER";
    public static final String RULE_NAME = "High Value Cross-Border Wire Transfer";

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

        BigDecimal crossBorderThreshold = config.getThresholdAmount() != null ? config.getThresholdAmount() : BigDecimal.valueOf(25000.00);

        if (txn.getCountryCode() != null && txn.getCounterpartyCountryCode() != null) {
            if (!txn.getCountryCode().equalsIgnoreCase(txn.getCounterpartyCountryCode())) {
                if (txn.getAmount() != null && txn.getAmount().compareTo(crossBorderThreshold) >= 0) {
                    String narrative = String.format("Cross-border transfer from %s to %s for $%s exceeded threshold $%s",
                            txn.getCountryCode(), txn.getCounterpartyCountryCode(), txn.getAmount(), crossBorderThreshold);

                    Alert alert = Alert.builder()
                            .alertId(UUID.randomUUID())
                            .tenantId(txn.getTenantId())
                            .transactionId(txn.getTransactionId())
                            .customerId(txn.getCustomerId())
                            .ruleCode(RULE_CODE)
                            .ruleName(RULE_NAME)
                            .severity(AlertSeverity.HIGH)
                            .triggeredAmount(txn.getAmount())
                            .narrative(narrative)
                            .detectionMetadataJson(String.format("{\"origin\":%s,\"destination\":%s,\"amount\":%s}",
                                    txn.getCountryCode(), txn.getCounterpartyCountryCode(), txn.getAmount()))
                            .isReviewed(false)
                            .createdAt(LocalDateTime.now())
                            .build();

                    return Optional.of(alert);
                }
            }
        }

        return Optional.empty();
    }
}
