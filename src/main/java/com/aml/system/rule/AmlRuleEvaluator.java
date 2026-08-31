package com.aml.system.rule;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Core Strategy Interface for Anti-Money Laundering (AML) Compliance Rule Evaluators.
 *
 * Implementations evaluate an incoming transaction in the context of tenant-specific threshold
 * configuration and customer historical transactions.
 */
public interface AmlRuleEvaluator {

    /**
     * Unique business rule code (e.g., 'AML_RULE_001_VELOCITY', 'AML_RULE_002_STRUCTURING').
     */
    String getRuleCode();

    /**
     * Human-readable rule name.
     */
    String getRuleName();

    /**
     * Evaluates a single transaction against rule criteria and historical window.
     *
     * @param txn The transaction currently being evaluated in the Spring Batch stream.
     * @param config The tenant-specific rule configuration and parameterized thresholds.
     * @param history Prior historical transactions for this customer within the lookback window.
     * @return Optional containing an Alert if suspicious behavior is flagged, otherwise Optional.empty().
     */
    Optional<Alert> evaluate(Transaction txn, TenantRuleConfig config, List<Transaction> history);
}
