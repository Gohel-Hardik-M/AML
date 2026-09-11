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
public class SmurfingNetworkRule implements AmlRule {

    private final TransactionRepository transactionRepository;

    public SmurfingNetworkRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String getRuleCode() {
        return "SMURFING_001";
    }

    @Override
    public String getRuleName() {
        return "Smurfing Layering Network";
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
        BigDecimal threshold = config.getThresholdAmount();
        int windowMinutes = config.getWindowMinutes();

        LocalDateTime from = transaction.getTimestamp().minusMinutes(windowMinutes);
        LocalDateTime to = transaction.getTimestamp();

        long count = context != null
            ? context.count(transaction.getCustomerID(), from, to)
            : transactionRepository.countCustomerTransactions(transaction.getCustomerID(), from, to);

        boolean amountExceeded = threshold != null && transaction.getAmount() != null
            && transaction.getAmount().compareTo(threshold) >= 0;

        if (count >= maxTransactions || amountExceeded) {
            return RuleEvaluationResult.triggered(
                    getRuleCode(),
                    getRuleName(),
                    "HIGH",
                    transaction.getAmount(),
                    "Customer transaction pattern detected: " + count + " transactions within "
                            + windowMinutes + " minutes (configured limit: " + maxTransactions + " txns)."
            );
        }

        return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
    }
}
