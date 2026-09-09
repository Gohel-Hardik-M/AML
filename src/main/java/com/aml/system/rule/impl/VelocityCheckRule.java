package com.aml.system.rule.impl;

import com.aml.system.model.Transaction;
import com.aml.system.repository.TransactionRepository;
import com.aml.system.rule.AmlRule;
import com.aml.system.rule.RuleEvaluationResult;
import org.hibernate.validator.constraints.CodePointLength;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class VelocityCheckRule implements AmlRule {


        private static final int MAX_TRANSACTIONS = 10;
        private static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("50000");

        private final TransactionRepository transactionRepository;

        public VelocityCheckRule(TransactionRepository transactionRepository) {
            this.transactionRepository = transactionRepository;
        }

        @Override
        public String getRuleCode() {
            return "VELOCITY_001";
        }

        @Override
        public String getRuleName() {
            return "Velocity Check";
        }

        @Override
        public RuleEvaluationResult evaluate(Transaction transaction) {

            if (transaction.getCustomerID() == null || transaction.getTimestamp() == null) {
                return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
            }

            LocalDateTime from = transaction.getTimestamp().minusHours(24);
            LocalDateTime to = transaction.getTimestamp();

            long transactionCount = transactionRepository.countCustomerTransactions(
                    transaction.getCustomerID(), from, to);

            BigDecimal totalAmount = transactionRepository.sumCustomerTransactions(
                    transaction.getCustomerID(), from, to);

            boolean excessiveFrequency = transactionCount >= MAX_TRANSACTIONS;
            boolean excessiveVolume = totalAmount.compareTo(MAX_TOTAL_AMOUNT) >= 0;

            if (excessiveFrequency || excessiveVolume) {
                return RuleEvaluationResult.triggered(
                        getRuleCode(), getRuleName(), "HIGH",
                        transaction.getAmount(),
                        "Customer activity exceeded the configured 24-hour velocity threshold."
                );
            }

            return RuleEvaluationResult.notTriggered(getRuleCode(), getRuleName());
        }


}
