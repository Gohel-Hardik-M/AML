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
 * Layering / Multi-Hop Rapid Conversion:
 * Multiple conversions across distinct payment rails (ACH -> Wire -> Cash) within a compressed timeframe.
 */
@Component
public class StructuringLayeringRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_014_LAYERING_CONVERSION";
    public static final String RULE_NAME = "Multi-Channel Layering & Rapid Payment Rail Hopping";

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

        long distinctChannels = history.stream()
                .filter(h -> h.getChannel() != null)
                .map(Transaction::getChannel)
                .distinct()
                .count();

        if (txn.getChannel() != null) {
            distinctChannels++;
        }

        if (distinctChannels >= 3 && txn.getAmount() != null && txn.getAmount().compareTo(BigDecimal.valueOf(20000)) > 0) {
            String narrative = String.format("Layering indicator: Customer used %d distinct payment channels in rolling window totaling $%s.",
                    distinctChannels, txn.getAmount());

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
                    .detectionMetadataJson(String.format("{\"distinctChannels\":%d}", distinctChannels))
                    .isReviewed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            return Optional.of(alert);
        }

        return Optional.empty();
    }
}
