package com.aml.system.service;


import com.aml.system.model.Alert;
import com.aml.system.model.AlertSeverity;
import com.aml.system.model.Transaction;
import com.aml.system.repository.AlertRepository;
import com.aml.system.rule.RuleEvaluationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AlertService {


        private final AlertRepository alertRepository;
        private final ObjectMapper objectMapper;

            public AlertService(AlertRepository alertRepository, ObjectMapper objectMapper) {
            this.alertRepository = alertRepository;
                this.objectMapper = objectMapper;
        }

        public void createAlerts(Transaction transaction, List<RuleEvaluationResult> results) {

            for (RuleEvaluationResult result : results) {

                Alert alert = Alert.builder()
                        .alertId(UUID.randomUUID())
                        .transactionId(transaction.getTransactionId())
                        .customerId(transaction.getCustomerID())
                        .ruleCode(result.getRuleCode())
                        .ruleName(result.getRuleName())
                        .severity(AlertSeverity.valueOf(result.getSeverity()))
                        .triggeredAmount(result.getTriggeredAmount())
                        .narrative(result.getNarrative())
                        .detectionMetadataJson(metadata(result))
                        .reviewed(false)
                        .batchId(transaction.getBatchId())
                        .createdAt(LocalDateTime.now())
                        .build();

                alertRepository.save(alert);
            }
        }

        private String metadata(RuleEvaluationResult result) {
            try {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("ruleCode", result.getRuleCode());
                values.put("ruleName", result.getRuleName());
                values.put("severity", result.getSeverity());
                values.put("triggeredAmount", result.getTriggeredAmount());
                values.put("narrative", result.getNarrative());
                return objectMapper.writeValueAsString(values);
            } catch (JsonProcessingException exception) {
                return "{}";
            }
        }



}
