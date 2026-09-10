package com.aml.system.rule.impl;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
        if (transaction.getAmount() == null || config == null || config.getThresholdAmount() == null) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }

        BigDecimal threshold = config.getThresholdAmount();

        boolean eligibleTransaction =
                transaction.getTransactionType() == TransactionType.CASH_DEPOSIT
                        || transaction.getTransactionType() == TransactionType.CASH_WITHDRAWAL;

        if (eligibleTransaction && transaction.getAmount().compareTo(threshold) >= 0) {
            return RuleEvaluationResult.triggered(
                    getRuleCode(),
                    getRuleName(),
                    "HIGH",
                    transaction.getAmount(),
                    "Cash transaction (" + transaction.getAmount() + ") meets or exceeds configured structuring threshold (" + threshold + ")."
            );
        }

        return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
    }
}
