package com.aml.system.rule.impl;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.rule.AmlRuleEvaluator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Detects sudden large activity on accounts that have been inactive/dormant for > 180 days.
 */
@Component
public class DormantAccountActivityRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_007_DORMANT_ACTIVITY";
    public static final String RULE_NAME = "Sudden Large Inflow on Dormant Account";

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

        BigDecimal minTriggerAmount = config.getThresholdAmount() != null ? config.getThresholdAmount() : BigDecimal.valueOf(10000.00);
        int dormantDaysThreshold = 180;

        if (txn.getAmount() == null || txn.getAmount().compareTo(minTriggerAmount) < 0) {
            return Optional.empty();
        }

        if (history != null && !history.isEmpty()) {
            // Find most recent prior transaction
            Optional<Transaction> latestPrior = history.stream()
                    .filter(h -> h.getTimestamp() != null && h.getTimestamp().isBefore(txn.getTimestamp()))
                    .max(Comparator.comparing(Transaction::getTimestamp));

            if (latestPrior.isPresent()) {
                long daysSinceLastTxn = ChronoUnit.DAYS.between(latestPrior.get().getTimestamp(), txn.getTimestamp());
                if (daysSinceLastTxn >= dormantDaysThreshold) {
                    String narrative = String.format("Dormant account reactivation: Account dormant for %d days received sudden transfer of $%s.",
                            daysSinceLastTxn, txn.getAmount().toPlainString());

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
                            .detectionMetadataJson(String.format("{\"dormantDays\":%d,\"amount\":%s}", daysSinceLastTxn, txn.getAmount()))
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
