package com.aml.system.batch;

import com.aml.system.model.Alert;
import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.repository.TenantRuleConfigRepository;
import com.aml.system.rule.AmlRuleEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =========================================================================================
 * ARCHITECTURAL DESIGN: SPRING BATCH CHUNK PROCESSOR WITH STRATEGY PATTERN
 * =========================================================================================
 * 
 * 1. CHUNK-ORIENTED PROCESSING PARADIGM:
 *    - In Spring Batch, records are read individually via ItemReader, passed one-by-one
 *      through this ItemProcessor, and accumulated into a fixed-size Chunk (e.g. 5,000 items).
 *    - Once the chunk is filled, ItemWriter commits the batch in a single database transaction.
 *    - This guarantees constant O(1) JVM memory utilization regardless of whether the file has
 *      10,000 or 10,000,000 records.
 * 
 * 2. STRATEGY PATTERN FOR 15 COMPLIANCE RULES:
 *    - Spring automatically autowires all 15 beans implementing `AmlRuleEvaluator` into the
 *      `List<AmlRuleEvaluator>` field.
 *    - The processor evaluates each transaction against all active tenant-configured rule strategies.
 *    - Extensibility is Open/Closed (SOLID): Adding a 16th rule requires zero code modifications
 *      to this processor; simply create a new `@Component implements AmlRuleEvaluator`.
 * 
 * 3. IN-MEMORY TENANT CONFIG CACHING:
 *    - Tenant rule thresholds are cached in a thread-safe ConcurrentHashMap to avoid querying
 *      PostgreSQL 150 million times during a 10M record batch run.
 * =========================================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngineProcessor implements ItemProcessor<Transaction, List<Alert>> {

    private final List<AmlRuleEvaluator> ruleEvaluators;
    private final TenantRuleConfigRepository tenantRuleConfigRepository;

    // Fast-path in-memory config cache per tenant & rule code
    private final Map<String, TenantRuleConfig> configCache = new ConcurrentHashMap<>();

    // Simulated short-term lookback cache for customer sliding windows during streaming
    private final Map<String, List<Transaction>> customerHistoryWindow = new ConcurrentHashMap<>();

    @Override
    public List<Alert> process(Transaction txn) {
        if (txn == null) {
            return Collections.emptyList();
        }

        List<Alert> triggeredAlerts = new ArrayList<>();
        String tenantId = txn.getTenantId();

        // 1. Retrieve customer's historical transactions for sliding window lookbacks
        String historyKey = tenantId + ":" + txn.getCustomerId();
        List<Transaction> history = customerHistoryWindow.computeIfAbsent(historyKey, k -> Collections.synchronizedList(new LinkedList<>()));

        // 2. Iterate through all 15 rules using the Strategy Pattern
        for (AmlRuleEvaluator rule : ruleEvaluators) {
            try {
                TenantRuleConfig ruleConfig = getTenantRuleConfig(tenantId, rule.getRuleCode());

                if (ruleConfig != null && Boolean.TRUE.equals(ruleConfig.getIsEnabled())) {
                    Optional<Alert> alertOpt = rule.evaluate(txn, ruleConfig, history);
                    alertOpt.ifPresent(triggeredAlerts::add);
                }
            } catch (Exception ex) {
                log.error("Rule evaluation exception on Rule [{}] for Txn [{}]: {}",
                        rule.getRuleCode(), txn.getTransactionId(), ex.getMessage(), ex);
            }
        }

        // 3. Maintain bounded history buffer (keep last 50 transactions to prevent memory leak)
        synchronized (history) {
            history.add(txn);
            if (history.size() > 50) {
                history.remove(0); // Evict oldest
            }
        }

        return triggeredAlerts;
    }

    /**
     * Cache-first lookup for tenant rule configuration thresholds.
     */
    private TenantRuleConfig getTenantRuleConfig(String tenantId, String ruleCode) {
        String cacheKey = tenantId + ":" + ruleCode;
        return configCache.computeIfAbsent(cacheKey, k ->
                tenantRuleConfigRepository.findByTenantIdAndRuleCode(tenantId, ruleCode)
                        .orElseGet(() -> TenantRuleConfig.builder()
                                .tenantId(tenantId)
                                .ruleCode(ruleCode)
                                .isEnabled(true)
                                .build())
        );
    }
}
