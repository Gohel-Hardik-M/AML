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
 * FATF High-Risk Jurisdictions (Blacklist / Greylist).
 * O(1) Set lookup.
 */
@Component
public class GeographicRiskRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_010_GEOGRAPHIC_RISK";
    public static final String RULE_NAME = "FATF High-Risk Jurisdiction Screening";

    private static final Set<String> FATF_HIGH_RISK_COUNTRIES = Set.of(
            "KP", "IR", "MM", "SY", "YE", "CU" // North Korea, Iran, Myanmar, Syria, Yemen, Cuba
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

        String dest = txn.getCounterpartyCountryCode() != null ? txn.getCounterpartyCountryCode().toUpperCase() : null;
        String orig = txn.getCountryCode() != null ? txn.getCountryCode().toUpperCase() : null;

        if ((dest != null && FATF_HIGH_RISK_COUNTRIES.contains(dest)) || (orig != null && FATF_HIGH_RISK_COUNTRIES.contains(orig))) {
            String country = FATF_HIGH_RISK_COUNTRIES.contains(dest) ? dest : orig;
            String narrative = String.format("Transaction involves FATF Call-for-Action High-Risk Country [%s]. Amount: $%s",
                    country, txn.getAmount());

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
                    .detectionMetadataJson(String.format("{\"countryCode\":\"%s\"}", country))
                    .isReviewed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            return Optional.of(alert);
        }

        return Optional.empty();
    }
}
