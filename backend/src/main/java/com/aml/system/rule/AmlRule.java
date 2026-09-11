package com.aml.system.rule;

import com.aml.system.model.TenantRuleConfig;
import com.aml.system.model.Transaction;

public interface AmlRule {

    String getRuleCode();

    String getRuleName();

    RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config);

    default RuleEvaluationResult evaluate(Transaction transaction, TenantRuleConfig config,
                                          RuleEvaluationContext context) {
        return evaluate(transaction, config);
    }
}
