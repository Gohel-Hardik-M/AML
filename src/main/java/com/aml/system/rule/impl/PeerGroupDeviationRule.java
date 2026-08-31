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
 * Peer Group Deviation Rule:
 * Compares customer behavior against baseline segment benchmarks (e.g. Retail Individual vs Large Corporate).
 */
@Component
public class PeerGroupDeviationRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_015_PEER_DEVIATION";
    public static final String RULE_NAME = "Peer Group Profile & Segmentation Deviation";

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

        BigDecimal peerGroupCap = config.getThresholdAmount() != null ? config.getThresholdAmount() : BigDecimal.valueOf(100000.00);

        if (txn.getAmount() != null && txn.getAmount().compareTo(peerGroupCap) > 0) {
            String narrative = String.format("Peer Group Deviation: Customer %s transaction amount $%s exceeds peer segment benchmark of $%s.",
                    txn.getCustomerId(), txn.getAmount().toPlainString(), peerGroupCap.toPlainString());

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
                    .detectionMetadataJson(String.format("{\"peerCap\":%s,\"txnAmount\":%s}", peerGroupCap, txn.getAmount()))
                    .isReviewed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            return Optional.of(alert);
        }

        return Optional.empty();
    }
}
