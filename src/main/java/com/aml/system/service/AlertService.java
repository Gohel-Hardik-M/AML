//package com.aml.system.service;
//
//
//import com.aml.system.model.Alert;
//import com.aml.system.model.AlertSeverity;
//import com.aml.system.model.Transaction;
//import com.aml.system.repository.AlertRepository;
//import com.aml.system.rule.RuleEvaluationResult;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//public class AlertService {
//
//
//
//        private final AlertRepository alertRepository;
//
//        public AlertService(AlertRepository alertRepository) {
//            this.alertRepository = alertRepository;
//        }
//
//        public void createAlerts(
//                Transaction transaction,
//                List<RuleEvaluationResult> results) {
//
//            for (RuleEvaluationResult result : results) {
//
//                Alert alert = Alert.builder()
//                        .alertId(UUID.randomUUID())
//                        .transactionId(transaction.getTransactionId())
//                        .customerId(transaction.getCustomerID())
//                        .ruleCode(result.getRuleCode())
//                        .ruleName(result.getRuleName())
//                        .severity(AlertSeverity.valueOf(result.getSeverity()))
//                        .triggeredAmount(result.getTriggeredAmount())
//                        .narrative(result.getNarrative())
//                        .detectionMetadataJson(null)
//                        .reviewed(false)
//                        .batchId(transaction.getBatchId())
//                        .tenantId(transaction.getTenantId())
//                        .createdAt(LocalDateTime.now())
//                        .build();
//
//                alertRepository.save(alert);
//            }
//        }
//
//}
