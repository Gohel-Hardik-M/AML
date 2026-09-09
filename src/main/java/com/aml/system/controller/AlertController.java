package com.aml.system.controller;

import com.aml.system.model.Alert;
import com.aml.system.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {


        private final AlertRepository alertRepository;

        public AlertController(AlertRepository alertRepository) {
            this.alertRepository = alertRepository;
        }

        @GetMapping
        public List<Alert> getAllAlerts() {
            return alertRepository.findAll();
        }

        @GetMapping("/unreviewed")
        public List<Alert> getUnreviewedAlerts() {
            return alertRepository.findByReviewedFalse();
        }

        @GetMapping("/batch/{batchId}")
        public List<Alert> getAlertsByBatch(@PathVariable UUID batchId) {
            return alertRepository.findByBatchId(batchId);
        }

}
