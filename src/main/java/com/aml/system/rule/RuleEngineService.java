package com.aml.system.rule;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.TenantRuleConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuleEngineService {

    private final List<AmlRule> rules;
    private final TenantRuleConfigRepository tenantRuleConfigRepository;

    public RuleEngineService(List<AmlRule> rules, TenantRuleConfigRepository tenantRuleConfigRepository) {
        this.rules = rules;
        this.tenantRuleConfigRepository = tenantRuleConfigRepository;
    }

    public List<RuleEvaluationResult> evaluate(Transaction transaction) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Rule evaluation attempted without tenant context.");
            return Collections.emptyList();
        }

        List<TenantRuleConfig> configs = tenantRuleConfigRepository.findByTenantId(tenantId);
        Map<String, TenantRuleConfig> activeConfigs = configs.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsEnabled()))
                .collect(Collectors.toMap(TenantRuleConfig::getRuleCode, c -> c, (a, b) -> a));

        return evaluate(transaction, activeConfigs);
    }

    public List<RuleEvaluationResult> evaluate(Transaction transaction,
                                               Map<String, TenantRuleConfig> activeConfigs) {
        return evaluate(transaction, activeConfigs, null);
    }

    public List<RuleEvaluationResult> evaluate(Transaction transaction,
                                               Map<String, TenantRuleConfig> activeConfigs,
                                               RuleEvaluationContext context) {
        if (activeConfigs == null || activeConfigs.isEmpty()) {
            log.debug("No active AML rules configured for transaction '{}'", transaction.getTransactionId());
            return Collections.emptyList();
        }

        return rules.stream()
                .filter(rule -> activeConfigs.containsKey(rule.getRuleCode()))
            .map(rule -> rule.evaluate(transaction, activeConfigs.get(rule.getRuleCode()), context))
                .filter(RuleEvaluationResult::isTriggered)
                .toList();
    }

    public boolean isSuspicious(Transaction transaction) {
        return !evaluate(transaction).isEmpty();
    }
}
