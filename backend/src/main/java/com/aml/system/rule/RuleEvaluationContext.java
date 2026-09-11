package com.aml.system.rule;

import com.aml.system.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RuleEvaluationContext {

    private final Map<String, List<Transaction>> transactionsByCustomer;

    public RuleEvaluationContext(Map<String, List<Transaction>> transactionsByCustomer) {
        this.transactionsByCustomer = transactionsByCustomer == null ? Map.of() : transactionsByCustomer;
    }

    public long count(String customerId, LocalDateTime from, LocalDateTime to) {
        return transactions(customerId, from, to).stream()
                .filter(transaction -> isWithin(transaction, from, to))
                .count();
    }

    public BigDecimal sum(String customerId, LocalDateTime from, LocalDateTime to) {
        return transactions(customerId, from, to).stream()
                .map(Transaction::getAmount)
            .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

        public List<Transaction> transactions(String customerId, LocalDateTime from, LocalDateTime to) {
        return transactionsByCustomer.getOrDefault(customerId, List.of()).stream()
            .filter(transaction -> isWithin(transaction, from, to))
            .toList();
        }

        public long count(String customerId, LocalDateTime from, LocalDateTime to, Predicate<Transaction> predicate) {
        return transactions(customerId, from, to).stream().filter(predicate).count();
        }

        public BigDecimal sum(String customerId, LocalDateTime from, LocalDateTime to, Predicate<Transaction> predicate) {
        return transactions(customerId, from, to).stream()
            .filter(predicate)
            .map(Transaction::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public Set<String> distinctSourceAccounts(String customerId, LocalDateTime from, LocalDateTime to,
                               Predicate<Transaction> predicate) {
        return transactions(customerId, from, to).stream()
            .filter(predicate)
            .map(Transaction::getSourceAccountId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        }

    private boolean isWithin(Transaction transaction, LocalDateTime from, LocalDateTime to) {
        return transaction != null && transaction.getTimestamp() != null
                && !transaction.getTimestamp().isBefore(from)
                && !transaction.getTimestamp().isAfter(to);
    }
}