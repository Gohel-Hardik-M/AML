package com.aml.system.rule.impl;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CryptoPatternRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "CRYPTO_001";
    }

    @Override
    public String getRuleName() {
        return "Cryptocurrency Transaction Pattern";
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config) {
        if (transaction.getAmount() == null || config == null || config.getThresholdAmount() == null) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }

        BigDecimal threshold = config.getThresholdAmount();

        boolean cryptoTransaction =
                transaction.getTransactionType() == TransactionType.CRYPTO_PURCHASE
                        || transaction.getTransactionType() == TransactionType.CRYPTO_DISBURSEMENT;

        if (cryptoTransaction && transaction.getAmount().compareTo(threshold) >= 0) {
            return RuleEvaluationResult.triggered(
                    getRuleCode(),
                    getRuleName(),
                    "MEDIUM",
                    transaction.getAmount(),
                    "Crypto-related transaction (" + transaction.getAmount()
                            + ") meets or exceeds the configured monitoring threshold (" + threshold + ")."
            );
        }

        return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
    }
}
