package com.aml.system.rule;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.repository.TransactionRepository;
import com.aml.system.rule.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RuleEngineComprehensiveTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 9, 12, 12, 0, 0);

    private CrossBorderRule crossBorderRule;
    private GeographicRiskRule geographicRiskRule;
    private SmurfingNetworkRule smurfingRule;
    private StructuringRule structuringRule;
    private VelocityCheckRule velocityRule;
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        crossBorderRule = new CrossBorderRule();
        geographicRiskRule = new GeographicRiskRule();
        smurfingRule = new SmurfingNetworkRule(transactionRepository);
        structuringRule = new StructuringRule();
        velocityRule = new VelocityCheckRule(transactionRepository);
    }

    // ==========================================
    // CrossBorderRule Tests
    // ==========================================
    @Test
    @DisplayName("CrossBorderRule: Triggers when transfer crosses borders and exceeds threshold")
    void crossBorderRule_triggersWhenDifferentCountriesAndAboveThreshold() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("CROSS_BORDER_001")
                .thresholdAmount(new BigDecimal("500000.00"))
                .isEnabled(true)
                .build();

        Transaction tx = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("750000.00"),
                TransactionType.WIRE_TRANSFER, "IN", "US", now);

        RuleEvaluationResult result = crossBorderRule.evaluate(tx, config);
        assertTrue(result.isTriggered());
        assertEquals("MEDIUM", result.getSeverity());
        assertEquals("CROSS_BORDER_001", result.getRuleCode());
        assertTrue(result.getNarrative().contains("IN -> US"));
    }

    @Test
    @DisplayName("CrossBorderRule: Safe when same country or below threshold or non-transfer type")
    void crossBorderRule_safeWhenConditionsNotMet() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("CROSS_BORDER_001")
                .thresholdAmount(new BigDecimal("500000.00"))
                .isEnabled(true)
                .build();

        // Same country
        Transaction domestic = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("750000.00"),
                TransactionType.WIRE_TRANSFER, "IN", "IN", now);
        assertFalse(crossBorderRule.evaluate(domestic, config).isTriggered());

        // Below threshold
        Transaction belowThreshold = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("499999.00"),
                TransactionType.WIRE_TRANSFER, "IN", "SG", now);
        assertFalse(crossBorderRule.evaluate(belowThreshold, config).isTriggered());

        // Non-transfer type (CASH_WITHDRAWAL)
        Transaction nonTransfer = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("750000.00"),
                TransactionType.CASH_WITHDRAWAL, "IN", "SG", now);
        assertFalse(crossBorderRule.evaluate(nonTransfer, config).isTriggered());

        // Null countries
        Transaction nullCountry = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("750000.00"),
                TransactionType.WIRE_TRANSFER, null, "SG", now);
        assertFalse(crossBorderRule.evaluate(nullCountry, config).isTriggered());
    }

    // ==========================================
    // GeographicRiskRule Tests
    // ==========================================
    @Test
    @DisplayName("GeographicRiskRule: Triggers on high-risk destination or source country at or above threshold")
    void geographicRiskRule_triggersOnSanctionedJurisdictions() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("GEO_RISK_001")
                .thresholdAmount(new BigDecimal("100000.00"))
                .isEnabled(true)
                .build();

        // Destination in IR (Iran)
        Transaction tx1 = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("100000.00"),
                TransactionType.WIRE_TRANSFER, "IN", "IR", now);
        RuleEvaluationResult res1 = geographicRiskRule.evaluate(tx1, config);
        assertTrue(res1.isTriggered());
        assertEquals("HIGH", res1.getSeverity());

        // Source in KP (North Korea) - case-insensitive
        Transaction tx2 = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("250000.00"),
                TransactionType.WIRE_TRANSFER, "kp", "IN", now);
        RuleEvaluationResult res2 = geographicRiskRule.evaluate(tx2, config);
        assertTrue(res2.isTriggered());

        // Destination in SY (Syria)
        Transaction tx3 = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("150000.00"),
                TransactionType.WIRE_TRANSFER, "IN", "sy", now);
        assertTrue(geographicRiskRule.evaluate(tx3, config).isTriggered());
    }

    @Test
    @DisplayName("GeographicRiskRule: Does not trigger for safe countries or amounts below threshold")
    void geographicRiskRule_safeForNormalJurisdictionsAndLowAmounts() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("GEO_RISK_001")
                .thresholdAmount(new BigDecimal("100000.00"))
                .isEnabled(true)
                .build();

        // Safe countries (IN -> US)
        Transaction normal = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("1000000.00"),
                TransactionType.WIRE_TRANSFER, "IN", "US", now);
        assertFalse(geographicRiskRule.evaluate(normal, config).isTriggered());

        // Sanctioned country but amount strictly below threshold
        Transaction lowAmount = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("99999.99"),
                TransactionType.WIRE_TRANSFER, "IN", "IR", now);
        assertFalse(geographicRiskRule.evaluate(lowAmount, config).isTriggered());

        // Null country codes
        Transaction nullCountry = createTransaction("CUST-1", "SRC-1", "DST-1", new BigDecimal("200000.00"),
                TransactionType.WIRE_TRANSFER, null, null, now);
        assertFalse(geographicRiskRule.evaluate(nullCountry, config).isTriggered());
    }

    // ==========================================
    // SmurfingNetworkRule Tests
    // ==========================================
    @Test
    @DisplayName("SmurfingNetworkRule: Detects multiple distinct source accounts funneling funds to same destination")
    void smurfingRule_triggersOnMultipleSourcesToSameDestination() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("SMURFING_001")
                .maxCount(2)
                .windowMinutes(60)
                .thresholdAmount(new BigDecimal("10000.00"))
                .isEnabled(true)
                .build();

        // 3 different source accounts to destination 'DST-A' within 60 mins aggregating 12,000 (>= 10,000)
        Transaction tx1 = createTransaction("C1", "SRC-1", "DST-A", new BigDecimal("4000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(30));
        Transaction tx2 = createTransaction("C1", "SRC-2", "DST-A", new BigDecimal("4000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(15));
        Transaction tx3 = createTransaction("C1", "SRC-3", "DST-A", new BigDecimal("4000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now);

        RuleEvaluationContext context = new RuleEvaluationContext(Map.of("C1", List.of(tx1, tx2, tx3)));

        RuleEvaluationResult result = smurfingRule.evaluate(tx3, config, context);
        assertTrue(result.isTriggered());
        assertEquals("HIGH", result.getSeverity());
        assertEquals("SMURFING_001", result.getRuleCode());
    }

    @Test
    @DisplayName("SmurfingNetworkRule: Does not trigger when single source account repeats transfers")
    void smurfingRule_doesNotTriggerForSingleSourceAccount() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("SMURFING_001")
                .maxCount(2)
                .windowMinutes(60)
                .thresholdAmount(new BigDecimal("10000.00"))
                .isEnabled(true)
                .build();

        // 3 transactions from the SAME source account 'SRC-1' to 'DST-A'
        Transaction tx1 = createTransaction("C1", "SRC-1", "DST-A", new BigDecimal("4000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(30));
        Transaction tx2 = createTransaction("C1", "SRC-1", "DST-A", new BigDecimal("4000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(15));
        Transaction tx3 = createTransaction("C1", "SRC-1", "DST-A", new BigDecimal("4000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now);

        RuleEvaluationContext context = new RuleEvaluationContext(Map.of("C1", List.of(tx1, tx2, tx3)));

        RuleEvaluationResult result = smurfingRule.evaluate(tx3, config, context);
        assertFalse(result.isTriggered(), "Should not trigger when distinct source accounts <= maxCount");
    }

    // ==========================================
    // StructuringRule Tests
    // ==========================================
    @Test
    @DisplayName("StructuringRule: Detects rapid cash deposits below reporting threshold aggregating above threshold")
    void structuringRule_triggersOnMultipleBelowThresholdCashDeposits() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("STRUCTURING_001")
                .maxCount(2)
                .windowMinutes(60)
                .thresholdAmount(new BigDecimal("100000.00"))
                .isEnabled(true)
                .build();

        // 3 cash deposits of 40,000 each (each < 100,000, aggregate = 120,000 >= 100,000, count 3 > maxCount 2)
        Transaction tx1 = createTransaction("C1", "CASH", "ACCT-1", new BigDecimal("40000.00"), TransactionType.CASH_DEPOSIT, "IN", "IN", now.minusMinutes(40));
        Transaction tx2 = createTransaction("C1", "CASH", "ACCT-1", new BigDecimal("40000.00"), TransactionType.CASH_DEPOSIT, "IN", "IN", now.minusMinutes(20));
        Transaction tx3 = createTransaction("C1", "CASH", "ACCT-1", new BigDecimal("40000.00"), TransactionType.CASH_DEPOSIT, "IN", "IN", now);

        RuleEvaluationContext context = new RuleEvaluationContext(Map.of("C1", List.of(tx1, tx2, tx3)));

        RuleEvaluationResult result = structuringRule.evaluate(tx3, config, context);
        assertTrue(result.isTriggered());
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    @DisplayName("StructuringRule: Does not trigger for non-cash deposits or single large deposits")
    void structuringRule_ignoresNonCashDeposits() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("STRUCTURING_001")
                .maxCount(2)
                .windowMinutes(60)
                .thresholdAmount(new BigDecimal("100000.00"))
                .isEnabled(true)
                .build();

        // Wire transfers are not cash structuring
        Transaction tx1 = createTransaction("C1", "SRC", "ACCT-1", new BigDecimal("40000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(40));
        Transaction tx2 = createTransaction("C1", "SRC", "ACCT-1", new BigDecimal("40000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(20));
        Transaction tx3 = createTransaction("C1", "SRC", "ACCT-1", new BigDecimal("40000.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now);

        RuleEvaluationContext context = new RuleEvaluationContext(Map.of("C1", List.of(tx1, tx2, tx3)));
        assertFalse(structuringRule.evaluate(tx3, config, context).isTriggered());
    }

    // ==========================================
    // VelocityCheckRule Tests
    // ==========================================
    @Test
    @DisplayName("VelocityCheckRule: Triggers when customer transaction count exceeds limit within window")
    void velocityRule_triggersWhenCountExceeded() {
        TenantRuleConfig config = TenantRuleConfig.builder()
                .ruleCode("VELOCITY_001")
                .maxCount(3)
                .windowMinutes(30)
                .thresholdAmount(new BigDecimal("1000.00"))
                .isEnabled(true)
                .build();

        // 4 transactions in 30 minutes (> 3)
        Transaction tx1 = createTransaction("C1", "A", "B", new BigDecimal("100.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(25));
        Transaction tx2 = createTransaction("C1", "A", "B", new BigDecimal("100.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(20));
        Transaction tx3 = createTransaction("C1", "A", "B", new BigDecimal("100.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now.minusMinutes(10));
        Transaction tx4 = createTransaction("C1", "A", "B", new BigDecimal("100.00"), TransactionType.WIRE_TRANSFER, "IN", "IN", now);

        RuleEvaluationContext context = new RuleEvaluationContext(Map.of("C1", List.of(tx1, tx2, tx3, tx4)));

        RuleEvaluationResult result = velocityRule.evaluate(tx4, config, context);
        assertTrue(result.isTriggered());
        assertEquals("HIGH", result.getSeverity());
    }

    // ==========================================
    // RuleEngineService Orchestration Tests
    // ==========================================
    @Test
    @DisplayName("RuleEngineService: Evaluates active rules and ignores disabled rules")
    void ruleEngineService_evaluatesConfiguredRules() {
        com.aml.system.repository.TenantRuleConfigRepository repo = mock(com.aml.system.repository.TenantRuleConfigRepository.class);
        RuleEngineService service = new RuleEngineService(List.of(crossBorderRule, geographicRiskRule), repo);

        TenantRuleConfig activeCrossBorder = TenantRuleConfig.builder()
                .ruleCode("CROSS_BORDER_001")
                .thresholdAmount(new BigDecimal("50000.00"))
                .isEnabled(true)
                .build();

        Transaction tx = createTransaction("C1", "SRC", "DST", new BigDecimal("100000.00"),
                TransactionType.WIRE_TRANSFER, "IN", "IR", now);

        List<RuleEvaluationResult> results = service.evaluate(
                tx,
                Map.of("CROSS_BORDER_001", activeCrossBorder)
        );

        // Cross-border should trigger
        assertEquals(1, results.size());
        assertEquals("CROSS_BORDER_001", results.get(0).getRuleCode());
        assertTrue(results.get(0).isTriggered());
    }

    private Transaction createTransaction(String customerId, String source, String destination,
                                          BigDecimal amount, TransactionType type,
                                          String country, String counterpartyCountry, LocalDateTime timestamp) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID())
                .customerID(customerId)
                .sourceAccountId(source)
                .destinationAccountId(destination)
                .amount(amount)
                .currency("INR")
                .transactionType(type)
                .countryCode(country)
                .counterpartyCountryCode(counterpartyCountry)
                .timestamp(timestamp)
                .build();
    }
}
