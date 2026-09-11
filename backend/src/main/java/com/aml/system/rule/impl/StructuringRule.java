package com.aml.system.rule.impl;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import com.aml.system.rule.RuleEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class StructuringRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "STRUCTURING_001";
    }

    @Override
    public String getRuleName() {
        return "Structuring / Smurfing Cash Deposits";
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config) {
        return evaluate(transaction, config, null);
        }

        @Override
        public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config, RuleEvaluationContext context) {
        if (transaction == null || transaction.getCustomerID() == null || transaction.getTimestamp() == null
            || config == null || config.getThresholdAmount() == null || config.getMaxCount() == null
            || config.getWindowMinutes() == null || context == null) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }

        BigDecimal threshold = config.getThresholdAmount();
        LocalDateTime from = transaction.getTimestamp().minusMinutes(config.getWindowMinutes());
        LocalDateTime to = transaction.getTimestamp();
        long count = context.count(transaction.getCustomerID(), from, to,
            candidate -> candidate.getTransactionType() == TransactionType.CASH_DEPOSIT);
        BigDecimal total = context.sum(transaction.getCustomerID(), from, to,
            candidate -> candidate.getTransactionType() == TransactionType.CASH_DEPOSIT);

        if (count > config.getMaxCount() && total.compareTo(threshold) > 0) {
            return RuleEvaluationResult.triggered(
                    getRuleCode(),
                    getRuleName(),
                    "HIGH",
                    transaction.getAmount(),
                    "" + count + " cash deposits totaling " + total + " exceeded the configured structuring threshold (" + threshold + ")."
            );
        }

        return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
    }
}
