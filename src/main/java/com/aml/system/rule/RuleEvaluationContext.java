package com.aml.system.rule;

import com.aml.system.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RuleEvaluationContext {

    private final Map<String, List<Transaction>> transactionsByCustomer;

    public RuleEvaluationContext(Map<String, List<Transaction>> transactionsByCustomer) {
        this.transactionsByCustomer = transactionsByCustomer;
    }

    public long count(String customerId, LocalDateTime from, LocalDateTime to) {
        return transactionsByCustomer.getOrDefault(customerId, List.of()).stream()
                .filter(transaction -> isWithin(transaction, from, to))
                .count();
    }

    public BigDecimal sum(String customerId, LocalDateTime from, LocalDateTime to) {
        return transactionsByCustomer.getOrDefault(customerId, List.of()).stream()
                .filter(transaction -> isWithin(transaction, from, to))
                .map(Transaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isWithin(Transaction transaction, LocalDateTime from, LocalDateTime to) {
        return transaction.getTimestamp() != null
                && !transaction.getTimestamp().isBefore(from)
                && !transaction.getTimestamp().isAfter(to);
    }
}