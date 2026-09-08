//package com.aml.system.rule.impl;
//
//import com.aml.system.model.Transaction;
//import com.aml.system.model.TransactionType;
//import com.aml.system.rule.AmlRule;
//import com.aml.system.rule.RuleEvaluationResult;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//
//@Component
//public class CryptoPatternRule implements AmlRule {
//
//
//        private static final BigDecimal THRESHOLD =
//                new BigDecimal("10000");
//
//        @Override
//        public String getRuleCode() {
//            return "CRYPTO_001";
//        }
//
//        @Override
//        public String getRuleName() {
//            return "Crypto Transaction Pattern";
//        }
//
//        @Override
//        public RuleEvaluationResult evaluate(Transaction transaction) {
//
//            if (transaction.getAmount() == null) {
//
//                return RuleEvaluationResult.notTriggered(
//                        getRuleCode(),
//                        getRuleName()
//                );
//            }
//
//            boolean cryptoTransaction =
//                    transaction.getTransactionType()
//                            == TransactionType.CRYPTO_PURCHASE
//                            ||
//                            transaction.getTransactionType()
//                                    == TransactionType.CRYPTO_DISBURSEMENT;
//
//            if (cryptoTransaction
//                    && transaction.getAmount()
//                    .compareTo(THRESHOLD) >= 0) {
//
//                return RuleEvaluationResult.triggered(
//                        getRuleCode(),
//                        getRuleName(),
//                        "MEDIUM",
//                        transaction.getAmount(),
//                        "Crypto-related transaction exceeds the configured monitoring threshold."
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
