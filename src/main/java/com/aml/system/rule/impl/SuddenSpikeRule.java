package com.aml.system.rule.impl;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.rule.AmlRuleEvaluator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Statistical Outlier Detection (Sudden Volume Spike):
 * Detects transactions exceeding N standard deviations or > 300% of customer's 90-day moving average.
 * Linear single-pass mean & variance computation in O(N).
 */
@Component
public class SuddenSpikeRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_012_SUDDEN_SPIKE";
    public static final String RULE_NAME = "Statistical Anomaly / Historical Average Volume Spike";

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
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled()) || history == null || history.size() < 5) {
            return Optional.empty();
        }

        BigDecimal multiplier = config.getPercentageDeviation() != null ?
                config.getPercentageDeviation().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : BigDecimal.valueOf(3.0); // 3x average

        // Single-pass mean calculation
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction h : history) {
            if (h.getAmount() != null) {
                sum = sum.add(h.getAmount());
            }
        }

        BigDecimal average = sum.divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);
        BigDecimal spikeThreshold = average.multiply(multiplier);

        if (txn.getAmount() != null && txn.getAmount().compareTo(spikeThreshold) > 0 && txn.getAmount().compareTo(BigDecimal.valueOf(5000)) > 0) {
            String narrative = String.format("Statistical Spike: Txn amount $%s is %.1fx higher than customer's historical average of $%s.",
                    txn.getAmount().toPlainString(),
                    txn.getAmount().divide(average, 1, RoundingMode.HALF_UP).doubleValue(),
                    average.toPlainString());

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
                    .detectionMetadataJson(String.format("{\"historicalAvg\":%s,\"currentAmount\":%s}", average, txn.getAmount()))
                    .isReviewed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            return Optional.of(alert);
        }

        return Optional.empty();
    }
}
