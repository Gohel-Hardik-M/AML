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
 * Crypto On-Ramp / High-Frequency VASP (Virtual Asset Service Provider) Settlement:
 * Flags large fiat-to-crypto gateway purchases exceeding threshold.
 */
@Component
public class CryptoOnRampRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_013_CRYPTO_ON_RAMP";
    public static final String RULE_NAME = "High-Value Crypto Gateway On-Ramp";

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

        BigDecimal cryptoThreshold = config.getThresholdAmount() != null ? config.getThresholdAmount() : BigDecimal.valueOf(15000.00);

        if (txn.getTransactionType() == TransactionType.CRYPTO_PURCHASE ||
                (txn.getCounterpartyName() != null && txn.getCounterpartyName().toUpperCase().contains("BINANCE")
                        || txn.getCounterpartyName() != null && txn.getCounterpartyName().toUpperCase().contains("COINBASE")
                        || txn.getCounterpartyName() != null && txn.getCounterpartyName().toUpperCase().contains("KRAKEN"))) {

            if (txn.getAmount() != null && txn.getAmount().compareTo(cryptoThreshold) >= 0) {
                String narrative = String.format("High value crypto asset acquisition: $%s deposited to crypto exchange %s.",
                        txn.getAmount(), txn.getCounterpartyName() != null ? txn.getCounterpartyName() : "VASP Gateway");

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
                        .detectionMetadataJson(String.format("{\"exchange\":\"%s\",\"amount\":%s}", txn.getCounterpartyName(), txn.getAmount()))
                        .isReviewed(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                return Optional.of(alert);
            }
        }

        return Optional.empty();
    }
}
