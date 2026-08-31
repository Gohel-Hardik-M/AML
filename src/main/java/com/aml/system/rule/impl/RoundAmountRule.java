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

/**
 * Detects frequent round figure transactions (e.g. exactly $10,000, $25,000, $50,000)
 * which are anomalous in retail trade and indicative of illicit off-ledger settlements.
 */
@Component
public class RoundAmountRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_004_ROUND_AMOUNT";
    public static final String RULE_NAME = "Suspicious Round Amount Transaction";

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

        BigDecimal minAmount = config.getThresholdAmount() != null ? config.getThresholdAmount() : BigDecimal.valueOf(5000.00);

        if (txn.getAmount() != null && txn.getAmount().compareTo(minAmount) >= 0) {
            BigDecimal remainder = txn.getAmount().remainder(BigDecimal.valueOf(1000));
            if (remainder.compareTo(BigDecimal.ZERO) == 0) {
                String narrative = String.format("Large round-figure transaction detected: Customer %s transferred exactly $%s",
                        txn.getCustomerId(), txn.getAmount().toPlainString());

                Alert alert = Alert.builder()
                        .alertId(UUID.randomUUID())
                        .tenantId(txn.getTenantId())
                        .transactionId(txn.getTransactionId())
                        .customerId(txn.getCustomerId())
                        .ruleCode(RULE_CODE)
                        .ruleName(RULE_NAME)
                        .severity(AlertSeverity.LOW)
                        .triggeredAmount(txn.getAmount())
                        .narrative(narrative)
                        .detectionMetadataJson(String.format("{\"amount\":%s,\"modulo\":1000}", txn.getAmount()))
                        .isReviewed(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                return Optional.of(alert);
            }
        }

        return Optional.empty();
    }
}
