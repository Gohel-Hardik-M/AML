package com.aml.system.rule.impl;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.rule.AmlRuleEvaluator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fan-In Aggregation (Many-to-One Smurfing Network):
 * Multiple distinct source accounts depositing funds into a single beneficiary within 24 hours.
 * Uses Hash Map frequency counting in O(N).
 */
@Component
public class SmurfingNetworkRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_011_SMURFING_NETWORK";
    public static final String RULE_NAME = "Many-to-One Fan-In Smurfing Aggregator";

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

        int maxDistinctSenders = (config.getMaxCount() != null) ? config.getMaxCount() : 5;
        Set<String> distinctSenders = new HashSet<>();
        BigDecimal totalDeposited = txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO;

        if (txn.getSourceAccountId() != null) {
            distinctSenders.add(txn.getSourceAccountId());
        }

        LocalDateTime windowStart = txn.getTimestamp() != null ? txn.getTimestamp().minusHours(24) : LocalDateTime.now().minusHours(24);

        for (Transaction h : history) {
            if (h.getTimestamp() != null && h.getTimestamp().isAfter(windowStart)) {
                if (h.getSourceAccountId() != null) {
                    distinctSenders.add(h.getSourceAccountId());
                }
                if (h.getAmount() != null) {
                    totalDeposited = totalDeposited.add(h.getAmount());
                }
            }
        }

        if (distinctSenders.size() >= maxDistinctSenders) {
            String narrative = String.format("Fan-in smurfing pattern: Destination account received funds from %d distinct source accounts totaling $%s in 24h.",
                    distinctSenders.size(), totalDeposited.toPlainString());

            Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID())
                    .tenantId(txn.getTenantId())
                    .transactionId(txn.getTransactionId())
                    .customerId(txn.getCustomerId())
                    .ruleCode(RULE_CODE)
                    .ruleName(RULE_NAME)
                    .severity(AlertSeverity.HIGH)
                    .triggeredAmount(totalDeposited)
                    .narrative(narrative)
                    .detectionMetadataJson(String.format("{\"distinctSenders\":%d,\"totalDeposited\":%s}",
                            distinctSenders.size(), totalDeposited.toPlainString()))
                    .isReviewed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            return Optional.of(alert);
        }

        return Optional.empty();
    }
}
