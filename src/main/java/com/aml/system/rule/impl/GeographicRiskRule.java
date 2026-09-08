//package com.aml.system.rule.impl;
//
//import com.aml.system.model.Transaction;
//import com.aml.system.rule.AmlRule;
//import com.aml.system.rule.RuleEvaluationResult;
//import org.springframework.stereotype.Component;
//
//import java.util.Set;
//
//@Component
//public class GeographicRiskRule implements AmlRule {
//
//
//        private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
//                "XX",
//                "YY",
//                "ZZ"
//        );
//
//        @Override
//        public String getRuleCode() {
//            return "GEO_RISK_001";
//        }
//
//        @Override
//        public String getRuleName() {
//            return "Geographic Risk";
//        }
//
//        @Override
//        public RuleEvaluationResult evaluate(Transaction transaction) {
//
//            String country =
//                    transaction.getCountryCode();
//
//            String counterpartyCountry =
//                    transaction.getCounterpartyCountryCode();
//
//            boolean countryRisk =
//                    country != null
//                            && HIGH_RISK_COUNTRIES.contains(
//                            country.toUpperCase()
//                    );
//
//            boolean counterpartyRisk =
//                    counterpartyCountry != null
//                            && HIGH_RISK_COUNTRIES.contains(
//                            counterpartyCountry.toUpperCase()
//                    );
//
//            if (countryRisk || counterpartyRisk) {
//
//                return RuleEvaluationResult.triggered(
//                        getRuleCode(),
//                        getRuleName(),
//                        "HIGH",
//                        transaction.getAmount(),
//                        "Transaction involves a configured high-risk jurisdiction."
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
