package com.aml.system.rule.impl;


import com.aml.system.model.Transaction;
import com.aml.system.model.TransactionType;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StructuringRule  implements AmlRule {



        private static final BigDecimal THRESHOLD =
                new BigDecimal("10000");

        @Override
        public String getRuleCode() {
            return "STRUCTURING_001";
        }

        @Override
        public String getRuleName() {
            return "Structuring / Smurfing";
        }

        @Override
        public RuleEvaluationResult evaluate(Transaction transaction) {

            if (transaction.getAmount() == null) {
                return RuleEvaluationResult.notTriggered(
                        getRuleCode(),
                        getRuleName()
                );
            }

            boolean eligibleTransaction =
                    transaction.getTransactionType() == TransactionType.CASH_DEPOSIT
                            || transaction.getTransactionType() == TransactionType.CASH_WITHDRAWAL;

            if (eligibleTransaction
                    && transaction.getAmount().compareTo(THRESHOLD) >= 0) {

                return RuleEvaluationResult.triggered(
                        getRuleCode(),
                        getRuleName(),
                        "HIGH",
                        transaction.getAmount(),
                        "Cash transaction meets or exceeds the configured structuring threshold."
                );
            }

            return RuleEvaluationResult.notTriggered(
                    getRuleCode(),
                    getRuleName()
            );
        }

}
