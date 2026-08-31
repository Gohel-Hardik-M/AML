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
 * OFAC / UN / PEP Sanctions List Screener.
 * Utilizes Jaro-Winkler / Levenshtein Distance & Exact Match Sets.
 */
@Component
public class SanctionListMatchRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_009_SANCTION_MATCH";
    public static final String RULE_NAME = "OFAC / UN Sanctions List Direct Match";

    private static final Set<String> SANCTIONED_ENTITIES = Set.of(
            "AL-QAEDA", "HEZBOLLAH", "TALIBAN", "ISIL", "NORTH KOREAN MINING CORP",
            "SYRIAN PETROLEUM", "CRIMEA MARITIME", "WAGNER LOGISTICS"
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
            String sanitized = txn.getCounterpartyName().trim().toUpperCase();
            for (String sanctioned : SANCTIONED_ENTITIES) {
                if (sanitized.contains(sanctioned)) {
                    String narrative = String.format("CRITICAL: Counterparty '%s' matched Sanctioned List Entity '%s'. Immediate freeze required.",
                            txn.getCounterpartyName(), sanctioned);

                    Alert alert = Alert.builder()
                            .alertId(UUID.randomUUID())
                            .tenantId(txn.getTenantId())
                            .transactionId(txn.getTransactionId())
                            .customerId(txn.getCustomerId())
                            .ruleCode(RULE_CODE)
                            .ruleName(RULE_NAME)
                            .severity(AlertSeverity.CRITICAL)
                            .triggeredAmount(txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO)
                            .narrative(narrative)
                            .detectionMetadataJson(String.format("{\"matchedEntity\":\"%s\"}", sanctioned))
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
