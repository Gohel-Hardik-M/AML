package com.aml.system.rule;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RuleEvaluationResult {



        private final boolean triggered;
        private final String ruleCode;
        private final String ruleName;
        private final String severity;
        private final BigDecimal triggeredAmount;
        private final String narrative;

        public static RuleEvaluationResult notTriggered(String ruleCode, String ruleName) {
            return RuleEvaluationResult.builder()
                    .triggered(false)
                    .ruleCode(ruleCode)
                    .ruleName(ruleName)
                    .build();
        }

        public static RuleEvaluationResult triggered(
                String ruleCode, String ruleName, String severity,
                BigDecimal triggeredAmount, String narrative) {
            return RuleEvaluationResult.builder()
                    .triggered(true)
                    .ruleCode(ruleCode)
                    .ruleName(ruleName)
                    .severity(severity)
                    .triggeredAmount(triggeredAmount)
                    .narrative(narrative)
                    .build();
        }

}
