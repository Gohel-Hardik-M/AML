//package com.aml.system.rule;
//
//
//import com.aml.system.model.Transaction;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class RuleEngineService {
//
//
//        private final List<AmlRule> rules;
//
//        public RuleEngineService(List<AmlRule> rules) {
//            this.rules = rules;
//        }
//
//        public List<RuleEvaluationResult> evaluate(Transaction transaction) {
//
//            return rules.stream()
//                    .map(rule -> rule.evaluate(transaction))
//                    .filter(RuleEvaluationResult::isTriggered)
//                    .toList();
//        }
//
//        public boolean isSuspicious(Transaction transaction) {
//            return !evaluate(transaction).isEmpty();
//        }
//
//}
