package com.aml.system.rule.impl;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.rule.AmlRuleEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * =========================================================================================
 * ALGORITHM & DATA STRUCTURE DESIGN: STRUCTURING / SMURFING DETECTION RULE
 * =========================================================================================
 * 
 * BUSINESS PURPOSE:
 * Identifies attempts to evade mandatory Currency Transaction Reporting (CTR) regulatory
 * thresholds (e.g. $10,000 threshold under FinCEN / RBI / FATF regulations) by breaking up large
 * cash deposits or wire transfers into multiple smaller amounts just below the reporting line
 * (e.g. 3 deposits of $9,500, $9,200, and $9,800 within a 48-hour window).
 * 
 * -----------------------------------------------------------------------------------------
 * WHY BRUTE FORCE (NESTED COMBINATIONS) FAILS AT 1 CRORE (10M) SCALE:
 * - Looking for clusters of sub-threshold transactions across a rolling 48-72 hour window using
 *   combinatorial subsets is O(2^N) or O(N^2) for nested ranges.
 * - Under 10M record batches, generating nested combination scans leads to CPU starvation.
 * 
 * -----------------------------------------------------------------------------------------
 * DATA STRUCTURE & ALGORITHM (DSA) SOLUTION:
 * - ALGORITHM: Two-Pointer Sliding Window with Bounded Filter Range & Prefix Sums.
 * - MECHANICS:
 *   1. Filter transactions to only consider candidate structuring amounts:
 *      lower_bound (e.g. 70% of CTR = $7,000) <= amount < upper_bound (e.g. 100% of CTR = $10,000).
 *   2. Maintain two pointers (L, R) across the filtered, sorted candidate list.
 *   3. Expand R to add next candidate transaction; add amount to window_structuring_sum.
 *   4. While (T_R - T_L > lookback_window_hours):
 *          window_structuring_sum -= Amount[L]
 *          L++
 *   5. If (R - L + 1 >= min_structuring_count) AND (window_structuring_sum >= aggregate_threshold):
 *          FLAG AML Alert for Structured Money Laundering / Smurfing.
 * - TIME COMPLEXITY: Strictly O(N) linear scan over candidate list.
 * - SPACE COMPLEXITY: O(K) where K is the number of sub-threshold transactions in window.
 * =========================================================================================
 */
@Slf4j
@Component
public class StructuringRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_002_STRUCTURING";
    public static final String RULE_NAME = "CTR Structuring & Smurfing Evasion Detection";

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public Optional<Alert> evaluate(Transaction txn, TenantRuleConfig config, List<Transaction> history) {
        if (config == null || !Boolean.TRUE.equals(config.getIsEnabled())) {
            return Optional.empty();
        }

        // Regulatory threshold (e.g. $10,000 CTR limit)
        BigDecimal ctrThreshold = (config.getThresholdAmount() != null) ? config.getThresholdAmount() : BigDecimal.valueOf(10000.00);
        // Structuring lower bound: 70% of CTR limit (e.g. $7,000 - $9,999.99)
        BigDecimal structuringLowerBound = ctrThreshold.multiply(BigDecimal.valueOf(0.70));
        int windowMinutes = (config.getWindowMinutes() != null && config.getWindowMinutes() > 0) ? config.getWindowMinutes() : 2880; // 48 hours
        int minCandidateCount = (config.getMaxCount() != null && config.getMaxCount() > 0) ? config.getMaxCount() : 3;

        List<Transaction> candidates = new ArrayList<>();

        if (isStructuringCandidate(txn, structuringLowerBound, ctrThreshold)) {
            candidates.add(txn);
        }

        if (history != null) {
            for (Transaction h : history) {
                if (isStructuringCandidate(h, structuringLowerBound, ctrThreshold)) {
                    candidates.add(h);
                }
            }
        }

        if (candidates.size() < minCandidateCount) {
            return Optional.empty();
        }

        // Sort candidate sequence chronologically: O(K log K) where K << N
        candidates.sort(Comparator.comparing(Transaction::getTimestamp));

        // =================================================================================
        // TWO-POINTER SLIDING WINDOW ALGORITHM - LINEAR TIME EVALUATION
        // =================================================================================
        int left = 0;
        BigDecimal runningStructuringSum = BigDecimal.ZERO;

        for (int right = 0; right < candidates.size(); right++) {
            Transaction current = candidates.get(right);
            runningStructuringSum = runningStructuringSum.add(current.getAmount());

            LocalDateTime windowStartBoundary = current.getTimestamp().minusMinutes(windowMinutes);
            while (left <= right && candidates.get(left).getTimestamp().isBefore(windowStartBoundary)) {
                runningStructuringSum = runningStructuringSum.subtract(candidates.get(left).getAmount());
                left++;
            }

            int countInWindow = right - left + 1;

            // If multiple sub-threshold transactions cumulatively exceed CTR threshold
            if (countInWindow >= minCandidateCount && runningStructuringSum.compareTo(ctrThreshold) >= 0) {
                String narrative = String.format(
                        "Potential Structuring / Smurfing pattern: Customer %s executed %d just-below-threshold transactions totaling $%s within %d hours (Aggregated total exceeds CTR threshold of $%s).",
                        txn.getCustomerId(),
                        countInWindow,
                        runningStructuringSum.toPlainString(),
                        windowMinutes / 60,
                        ctrThreshold.toPlainString()
                );

                Alert alert = Alert.builder()
                        .alertId(UUID.randomUUID())
                        .tenantId(txn.getTenantId())
                        .transactionId(txn.getTransactionId())
                        .customerId(txn.getCustomerId())
                        .ruleCode(RULE_CODE)
                        .ruleName(RULE_NAME)
                        .severity(AlertSeverity.CRITICAL)
                        .triggeredAmount(runningStructuringSum)
                        .narrative(narrative)
                        .detectionMetadataJson(String.format(
                                "{\"ctrThreshold\":%s,\"detectedCount\":%d,\"aggregateAmount\":%s,\"windowHours\":%d}",
                                ctrThreshold.toPlainString(), countInWindow, runningStructuringSum.toPlainString(), windowMinutes / 60
                        ))
                        .isReviewed(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                return Optional.of(alert);
            }
        }

        return Optional.empty();
    }

    private boolean isStructuringCandidate(Transaction txn, BigDecimal lowerBound, BigDecimal upperBound) {
        if (txn == null || txn.getAmount() == null || txn.getTimestamp() == null) {
            return false;
        }
        return txn.getAmount().compareTo(lowerBound) >= 0 && txn.getAmount().compareTo(upperBound) < 0;
    }
}
