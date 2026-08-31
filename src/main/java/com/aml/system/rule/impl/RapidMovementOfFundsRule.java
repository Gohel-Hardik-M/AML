package com.aml.system.rule.impl;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.model.enums.TransactionType;
import com.aml.system.rule.AmlRuleEvaluator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rapid Movement of Funds (Pass-Through / Mule Account):
 * Inflow closely followed by almost identical outflow within hours with negligible balance retention.
 */
@Component
public class RapidMovementOfFundsRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_008_RAPID_MOVEMENT";
    public static final String RULE_NAME = "Rapid Movement of Funds / Mule Account";

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
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled()) || history == null || history.isEmpty()) {
            return Optional.empty();
        }

        int maxHoursWindow = (config.getWindowMinutes() != null) ? config.getWindowMinutes() / 60 : 24;
        BigDecimal tolerancePercentage = BigDecimal.valueOf(0.05); // within 5% amount match

        if (txn.getAmount() == null || txn.getTimestamp() == null) {
            return Optional.empty();
        }

        for (Transaction prior : history) {
            if (prior.getTimestamp() == null || prior.getAmount() == null) continue;

            long hoursApart = java.time.Duration.between(prior.getTimestamp(), txn.getTimestamp()).abs().toHours();
            if (hoursApart <= maxHoursWindow) {
                BigDecimal diff = txn.getAmount().subtract(prior.getAmount()).abs();
                BigDecimal maxAllowedDiff = prior.getAmount().multiply(tolerancePercentage);

                if (diff.compareTo(maxAllowedDiff) <= 0 && prior.getAmount().compareTo(BigDecimal.valueOf(10000)) >= 0) {
                    String narrative = String.format("Rapid pass-through funds: Outflow of $%s matched recent inflow of $%s within %d hours.",
                            txn.getAmount(), prior.getAmount(), hoursApart);

                    Alert alert = Alert.builder()
                            .alertId(UUID.randomUUID())
                            .tenantId(txn.getTenantId())
                            .transactionId(txn.getTransactionId())
                            .customerId(txn.getCustomerId())
                            .ruleCode(RULE_CODE)
                            .ruleName(RULE_NAME)
                            .severity(AlertSeverity.CRITICAL)
                            .triggeredAmount(txn.getAmount())
                            .narrative(narrative)
                            .detectionMetadataJson(String.format("{\"inflow\":%s,\"outflow\":%s,\"hours\":%d}",
                                    prior.getAmount(), txn.getAmount(), hoursApart))
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
