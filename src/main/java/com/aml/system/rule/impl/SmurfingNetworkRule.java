//package com.aml.system.rule.impl;
//
//
//import com.aml.system.model.Transaction;
//import com.aml.system.repository.TransactionRepository;
//import com.aml.system.rule.AmlRule;
//import com.aml.system.rule.RuleEvaluationResult;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//public class SmurfingNetworkRule  implements AmlRule {
//
//
//        private static final int MAX_TRANSACTIONS =
//                5;
//
//        private final TransactionRepository transactionRepository;
//
//        public SmurfingNetworkRule(
//                TransactionRepository transactionRepository) {
//
//            this.transactionRepository = transactionRepository;
//        }
//
//        @Override
//        public String getRuleCode() {
//            return "SMURFING_001";
//        }
//
//        @Override
//        public String getRuleName() {
//            return "Smurfing Network";
//        }
//
//        @Override
//        public RuleEvaluationResult evaluate(Transaction transaction) {
//
//            if (transaction.getCustomerID() == null
//                    || transaction.getTimestamp() == null
//                    || transaction.getTenantId() == null) {
//
//                return RuleEvaluationResult.notTriggered(
//                        getRuleCode(),
//                        getRuleName()
//                );
//            }
//
//            LocalDateTime from =
//                    transaction.getTimestamp().minusHours(24);
//
//            LocalDateTime to =
//                    transaction.getTimestamp();
//
//            long count =
//                    transactionRepository.countCustomerTransactions(
//                            transaction.getCustomerID(),
//                            from,
//                            to
//                    );
//
//            if (count >= MAX_TRANSACTIONS) {
//
//                return RuleEvaluationResult.triggered(
//                        getRuleCode(),
//                        getRuleName(),
//                        "HIGH",
//                        transaction.getAmount(),
//                        "Multiple transactions associated with the customer were detected within the configured time window."
//                );
//            }
//
//            return RuleEvaluationResult.notTriggered(
//                    getRuleCode(),
//                    getRuleName()
//            );
//        }
//
//}
