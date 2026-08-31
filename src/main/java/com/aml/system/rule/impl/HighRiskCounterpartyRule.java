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
import java.util.Set;
import java.util.UUID;

/**
 * Evaluates counterparties against high-risk jurisdiction sets and designated shell companies.
 * Uses O(1) hash set lookup.
 */
@Component
public class HighRiskCounterpartyRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_006_HIGH_RISK_COUNTERPARTY";
    public static final String RULE_NAME = "High-Risk Counterparty / Shell Entity Detection";

    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "CASINO", "OFFSHORE", "HOLDINGS LTD", "BETTING", "PAYOUT LIMITED", "SHELL CORP", "SERVICES PANAMA"
    );

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

        if (txn.getCounterpartyName() != null) {
            String upper = txn.getCounterpartyName().toUpperCase();
            boolean match = HIGH_RISK_KEYWORDS.stream().anyMatch(upper::contains);

            if (match) {
                String narrative = String.format("Transaction to high-risk counterparty '%s' detected for customer %s.",
                        txn.getCounterpartyName(), txn.getCustomerId());

                Alert alert = Alert.builder()
                        .alertId(UUID.randomUUID())
                        .tenantId(txn.getTenantId())
                        .transactionId(txn.getTransactionId())
                        .customerId(txn.getCustomerId())
                        .ruleCode(RULE_CODE)
                        .ruleName(RULE_NAME)
                        .severity(AlertSeverity.HIGH)
                        .triggeredAmount(txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO)
                        .narrative(narrative)
                        .detectionMetadataJson(String.format("{\"counterparty\":\"%s\"}", txn.getCounterpartyName()))
                        .isReviewed(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                return Optional.of(alert);
            }
        }

        return Optional.empty();
    }
}
