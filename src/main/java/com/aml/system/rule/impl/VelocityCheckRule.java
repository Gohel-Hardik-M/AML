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
 * ALGORITHM & DATA STRUCTURE DESIGN: VELOCITY CHECK RULE
 * =========================================================================================
 * 
 * BUSINESS PURPOSE:
 * Detects rapid high-frequency transaction bursts on an account within a rolling time window
 * (e.g. > 5 transactions or > $50,000 total transferred within any 60-minute window).
 * 
 * -----------------------------------------------------------------------------------------
 * WHY BRUTE FORCE (NESTED LOOPS) FAILS AT 1 CRORE (10M) SCALE:
 * - A naive nested loop takes every transaction i and compares it against all other transactions j
 *   in the customer's history.
 * - Time Complexity: O(N^2) where N is the transaction history length.
 * - If a high-volume merchant or mule account has 5,000 transactions in the lookback window,
 *   evaluating 10,000,000 records with nested loops causes 25,000,000 comparisons per high-frequency
 *   account, freezing the JVM thread and blowing the batch job SLA.
 * 
 * -----------------------------------------------------------------------------------------
 * DATA STRUCTURE & ALGORITHM (DSA) SOLUTION:
 * - ALGORITHM: Sliding Window Technique using Two-Pointers (or ArrayDeque).
 * - PREREQUISITE: History is sorted chronologically by timestamp in O(N log N) or naturally
 *   ordered during database retrieval / ingestion streaming.
 * - MECHANICS:
 *   1. Left pointer (L) and Right pointer (R) define the active time window [T_left, T_right].
 *   2. As pointer R advances to include the latest transaction:
 *      While (T_R - T_L > window_duration_in_minutes):
 *          Deduct Amount[L] from running_window_sum
 *          Increment L (shrink window from the left)
 *   3. Current window size = (R - L + 1). Running Sum = running_window_sum.
 *   4. Check if (window_size > max_count) OR (running_window_sum > max_volume_threshold).
 * - TIME COMPLEXITY: Strictly O(N) linear time. Each transaction enters the sliding window once
 *   at pointer R and exits at pointer L at most once.
 * - SPACE COMPLEXITY: O(1) auxiliary space (using indexed list pointers) or O(K) where K is max
 *   events in the window.
 * =========================================================================================
 */
@Slf4j
@Component
public class VelocityCheckRule implements AmlRuleEvaluator {

    public static final String RULE_CODE = "AML_RULE_001_VELOCITY";
    public static final String RULE_NAME = "Rapid Transaction Velocity & Burst Detection";

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

        // Configure thresholds (with enterprise defaults)
        int windowMinutes = (config.getWindowMinutes() != null && config.getWindowMinutes() > 0) ? config.getWindowMinutes() : 60;
        int maxAllowedCount = (config.getMaxCount() != null && config.getMaxCount() > 0) ? config.getMaxCount() : 5;
        BigDecimal maxAllowedVolume = (config.getThresholdAmount() != null) ? config.getThresholdAmount() : BigDecimal.valueOf(50000.00);

        if (txn.getTimestamp() == null) {
            txn.setTimestamp(LocalDateTime.now());
        }

        // Combine current txn with history for complete rolling evaluation
        List<Transaction> fullSequence = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            fullSequence.addAll(history);
        }
        fullSequence.add(txn);

        // Sort sequence chronologically if not already ordered: O(N log N)
        fullSequence.sort(Comparator.comparing(Transaction::getTimestamp));

        // =================================================================================
        // TWO-POINTER SLIDING WINDOW ALGORITHM - O(N) LINEAR TIME EVALUATION
        // =================================================================================
        int left = 0;
        BigDecimal runningWindowSum = BigDecimal.ZERO;
        int maxObservedCountInAnyWindow = 0;
        BigDecimal maxObservedVolumeInAnyWindow = BigDecimal.ZERO;
        LocalDateTime windowStartTime = null;
        LocalDateTime windowEndTime = null;

        for (int right = 0; right < fullSequence.size(); right++) {
            Transaction currentTxn = fullSequence.get(right);
            runningWindowSum = runningWindowSum.add(currentTxn.getAmount() != null ? currentTxn.getAmount() : BigDecimal.ZERO);

            // Evict transactions from the left that have fallen outside the rolling time window
            LocalDateTime windowThreshold = currentTxn.getTimestamp().minusMinutes(windowMinutes);
            while (left <= right && fullSequence.get(left).getTimestamp().isBefore(windowThreshold)) {
                BigDecimal leftAmount = fullSequence.get(left).getAmount() != null ? fullSequence.get(left).getAmount() : BigDecimal.ZERO;
                runningWindowSum = runningWindowSum.subtract(leftAmount);
                left++;
            }

            int currentWindowCount = right - left + 1;

            if (currentWindowCount > maxObservedCountInAnyWindow) {
                maxObservedCountInAnyWindow = currentWindowCount;
            }
            if (runningWindowSum.compareTo(maxObservedVolumeInAnyWindow) > 0) {
                maxObservedVolumeInAnyWindow = runningWindowSum;
            }

            // Trigger detection if velocity threshold breached within window
            if (currentWindowCount > maxAllowedCount || runningWindowSum.compareTo(maxAllowedVolume) > 0) {
                windowStartTime = fullSequence.get(left).getTimestamp();
                windowEndTime = currentTxn.getTimestamp();

                String narrative = String.format(
                        "Velocity breach detected: Customer %s executed %d transactions totaling $%s within %d minutes (Window: %s to %s). Limit: %d txns / $%s.",
                        txn.getCustomerId(),
                        currentWindowCount,
                        runningWindowSum.toPlainString(),
                        windowMinutes,
                        windowStartTime,
                        windowEndTime,
                        maxAllowedCount,
                        maxAllowedVolume.toPlainString()
                );

                Alert alert = Alert.builder()
                        .alertId(UUID.randomUUID())
                        .tenantId(txn.getTenantId())
                        .transactionId(txn.getTransactionId())
                        .customerId(txn.getCustomerId())
                        .ruleCode(RULE_CODE)
                        .ruleName(RULE_NAME)
                        .severity(AlertSeverity.HIGH)
                        .triggeredAmount(runningWindowSum)
                        .narrative(narrative)
                        .detectionMetadataJson(String.format("{\"windowMinutes\":%d,\"txnCount\":%d,\"totalVolume\":%s}",
                                windowMinutes, currentWindowCount, runningWindowSum.toPlainString()))
                        .isReviewed(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                return Optional.of(alert);
            }
        }

        return Optional.empty();
    }
}
