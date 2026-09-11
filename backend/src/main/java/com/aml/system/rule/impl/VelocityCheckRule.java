package com.aml.system.rule.impl;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.repository.TransactionRepository;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import com.aml.system.rule.RuleEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class VelocityCheckRule implements AmlRule {

    private final TransactionRepository transactionRepository;

    public VelocityCheckRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String getRuleCode() {
        return "VELOCITY_001";
    }

    @Override
    public String getRuleName() {
        return "Rapid Transaction Velocity Check";
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config) {
        return evaluate(transaction, config, null);
    }

    @Override
    public RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config, RuleEvaluationContext context) {
        if (transaction.getCustomerID() == null || transaction.getTimestamp() == null || config == null
            || config.getMaxCount() == null || config.getWindowMinutes() == null) {
            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }

        int maxTransactions = config.getMaxCount();
        int windowMinutes = config.getWindowMinutes();
        BigDecimal maxTotalAmount = config.getThresholdAmount();

        LocalDateTime from = transaction.getTimestamp().minusMinutes(windowMinutes);
        LocalDateTime to = transaction.getTimestamp();

        long transactionCount = context != null
            ? context.count(transaction.getCustomerID(), from, to)
            : transactionRepository.countCustomerTransactions(transaction.getCustomerID(), from, to);

        BigDecimal totalAmount = context != null
            ? context.sum(transaction.getCustomerID(), from, to)
            : transactionRepository.sumCustomerTransactions(transaction.getCustomerID(), from, to);

        boolean excessiveFrequency = maxTransactions > 0 && transactionCount > maxTransactions;
        boolean excessiveVolume = maxTotalAmount != null && totalAmount != null
            && totalAmount.compareTo(maxTotalAmount) > 0;

        if (excessiveFrequency || excessiveVolume) {
            return RuleEvaluationResult.triggered(
                    getRuleCode(),
                    getRuleName(),
                    "HIGH",
                    transaction.getAmount(),
                    "Customer activity (" + transactionCount + " txns totaling " + totalAmount
                            + ") exceeded the configured velocity threshold (max " + maxTransactions
                            + " txns / " + maxTotalAmount + " in " + windowMinutes + " mins)."
            );
        }

        return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
    }
}
