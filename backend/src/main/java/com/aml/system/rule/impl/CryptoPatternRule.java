package com.aml.system.rule.impl;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationContext;
import com.aml.system.rule.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        return evaluate(transaction, config, null);
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config, RuleEvaluationContext context) {
        if (transaction == null || transaction.getAmount() == null || config == null
                || config.getThresholdAmount() == null || !isCrypto(transaction)) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }
        BigDecimal total = transaction.getAmount();
        long count = 1;
        boolean exceeded;
        if (context != null && transaction.getCustomerID() != null && transaction.getTimestamp() != null) {
            int window = config.getWindowMinutes() == null ? 0 : Math.max(0, config.getWindowMinutes());
            LocalDateTime from = transaction.getTimestamp().minusMinutes(window);
            count = context.count(transaction.getCustomerID(), from, transaction.getTimestamp(), this::isCrypto);
            total = context.sum(transaction.getCustomerID(), from, transaction.getTimestamp(), this::isCrypto);
            exceeded = total.compareTo(config.getThresholdAmount()) > 0
                    || (config.getMaxCount() != null && count > config.getMaxCount());
        } else {
            exceeded = transaction.getAmount().compareTo(config.getThresholdAmount()) > 0;
        }
        if (!exceeded) return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        return RuleEvaluationResult.triggered(getRuleCode(), getRuleName(), "MEDIUM", transaction.getAmount(),
                count + " crypto transactions totaling " + total
                        + " exceeded the configured monitoring threshold (" + config.getThresholdAmount() + ").");
    }

    private boolean isCrypto(Transaction transaction) {
        return transaction.getTransactionType() == TransactionType.CRYPTO_PURCHASE
                || transaction.getTransactionType() == TransactionType.CRYPTO_DISBURSEMENT;
    }
}
