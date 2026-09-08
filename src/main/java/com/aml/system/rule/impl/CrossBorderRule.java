//package com.aml.system.rule.impl;
//
//import com.aml.system.model.Transaction;
//import com.aml.system.rule.AmlRule;
//import com.aml.system.rule.RuleEvaluationResult;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//
//@Component
//public class CrossBorderRule  implements AmlRule {
//
//        private static final BigDecimal THRESHOLD =
//                new BigDecimal("25000");
//
//        @Override
//        public String getRuleCode() {
//            return "CROSS_BORDER_001";
//        }
//
//        @Override
//        public String getRuleName() {
//            return "Cross Border Transaction";
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
//            String country =
//                    transaction.getCountryCode();
//
//            String counterpartyCountry =
//                    transaction.getCounterpartyCountryCode();
//
//            if (country == null || counterpartyCountry == null) {
//
//                return RuleEvaluationResult.notTriggered(
//                        getRuleCode(),
//                        getRuleName()
//                );
//            }
//
//            boolean differentCountries =
//                    !country.equalsIgnoreCase(counterpartyCountry);
//
//            boolean largeAmount =
//                    transaction.getAmount()
//                            .compareTo(THRESHOLD) >= 0;
//
//            if (differentCountries && largeAmount) {
//
//                return RuleEvaluationResult.triggered(
//                        getRuleCode(),
//                        getRuleName(),
//                        "MEDIUM",
//                        transaction.getAmount(),
//                        "Large transaction involves different origin and counterparty jurisdictions."
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
