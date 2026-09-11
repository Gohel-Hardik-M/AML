package com.aml.system.rule;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.rule.impl.SmurfingNetworkRule;
import com.aml.system.rule.impl.StructuringRule;
import com.aml.system.rule.impl.VelocityCheckRule;
import com.aml.system.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RuleEngineRulesTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 11, 12, 0);

    @Test
    void velocityTriggersOnlyAfterConfiguredCountIsExceeded() {
        VelocityCheckRule rule = new VelocityCheckRule(mock(TransactionRepository.class));
        TenantRuleConfig config = config("VELOCITY_001", 5, 60, new BigDecimal("100000"));
        Transaction current = transaction("C1", "A3", "B3", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now);
        Map<String, List<Transaction>> data = Map.of("C1", List.of(
                transaction("C1", "A1", "B1", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now.minusMinutes(2)),
                transaction("C1", "A2", "B2", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now.minusMinutes(1)),
                current));

        assertFalse(rule.evaluate(current, config, new RuleEvaluationContext(data)).isTriggered());
        data = Map.of("C1", List.of(
                transaction("C1", "A1", "B1", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now.minusMinutes(4)),
                transaction("C1", "A2", "B2", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now.minusMinutes(3)),
                transaction("C1", "A3", "B3", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now.minusMinutes(2)),
                transaction("C1", "A4", "B4", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now.minusMinutes(1)),
                transaction("C1", "A5", "B5", new BigDecimal("10"), TransactionType.WIRE_TRANSFER, now)));
        assertFalse(rule.evaluate(current, config, new RuleEvaluationContext(data)).isTriggered());
    }

    @Test
    void structuringRequiresMultipleBelowThresholdCashDepositsAndAggregate() {
        StructuringRule rule = new StructuringRule();
        TenantRuleConfig config = config("STRUCTURING_001", 3, 60, new BigDecimal("10000"));
        Transaction current = transaction("C1", "A3", "B3", new BigDecimal("3000"), TransactionType.CASH_DEPOSIT, now);
        RuleEvaluationContext context = new RuleEvaluationContext(Map.of("C1", List.of(
                transaction("C1", "A1", "B1", new BigDecimal("3500"), TransactionType.CASH_DEPOSIT, now.minusMinutes(2)),
                transaction("C1", "A2", "B2", new BigDecimal("3500"), TransactionType.CASH_DEPOSIT, now.minusMinutes(1)),
                current)));

        assertFalse(rule.evaluate(current, config, context).isTriggered());
        Transaction fourth = transaction("C1", "A4", "B4", new BigDecimal("3000"), TransactionType.CASH_DEPOSIT, now);
        RuleEvaluationContext exceeded = new RuleEvaluationContext(Map.of("C1", List.of(
                transaction("C1", "A1", "B1", new BigDecimal("3500"), TransactionType.CASH_DEPOSIT, now.minusMinutes(3)),
                transaction("C1", "A2", "B2", new BigDecimal("3500"), TransactionType.CASH_DEPOSIT, now.minusMinutes(2)),
                transaction("C1", "A3", "B3", new BigDecimal("3000"), TransactionType.CASH_DEPOSIT, now.minusMinutes(1)),
                fourth)));
        assertTrue(rule.evaluate(fourth, config, exceeded).isTriggered());
    }

    @Test
    void smurfingRequiresMultipleSourceAccounts() {
        SmurfingNetworkRule rule = new SmurfingNetworkRule(mock(TransactionRepository.class));
        TenantRuleConfig config = config("SMURFING_001", 2, 60, new BigDecimal("10000"));
        Transaction current = transaction("C1", "A2", "B", new BigDecimal("4000"), TransactionType.WIRE_TRANSFER, now);
        RuleEvaluationContext context = new RuleEvaluationContext(Map.of("C1", List.of(
                transaction("C1", "A1", "B", new BigDecimal("4000"), TransactionType.WIRE_TRANSFER, now.minusMinutes(1)),
                transaction("C1", "A2", "B", new BigDecimal("4000"), TransactionType.WIRE_TRANSFER, now))));

        assertFalse(rule.evaluate(current, config, context).isTriggered());
        RuleEvaluationContext exceeded = new RuleEvaluationContext(Map.of("C1", List.of(
                transaction("C1", "A1", "B", new BigDecimal("4000"), TransactionType.WIRE_TRANSFER, now.minusMinutes(2)),
                transaction("C1", "A2", "B", new BigDecimal("4000"), TransactionType.WIRE_TRANSFER, now.minusMinutes(1)),
                transaction("C1", "A3", "B", new BigDecimal("4000"), TransactionType.WIRE_TRANSFER, now))));
        assertTrue(rule.evaluate(current, config, exceeded).isTriggered());
    }

    private TenantRuleConfig config(String code, int maxCount, int window, BigDecimal threshold) {
        return TenantRuleConfig.builder().ruleCode(code).isEnabled(true).maxCount(maxCount)
                .windowMinutes(window).thresholdAmount(threshold).build();
    }

    private Transaction transaction(String customer, String source, String destination, BigDecimal amount,
                                    TransactionType type, LocalDateTime timestamp) {
        return Transaction.builder().transactionId(UUID.randomUUID()).customerID(customer)
                .sourceAccountId(source).destinationAccountId(destination).amount(amount)
                .currency("USD").transactionType(type).timestamp(timestamp).build();
    }
}
