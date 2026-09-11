package com.aml.system.controller;

import com.aml.system.model.Alert;
import com.aml.system.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {


        private final AlertRepository alertRepository;

        public AlertController(AlertRepository alertRepository) {
            this.alertRepository = alertRepository;
        }

        @GetMapping
        public Page<Alert> getAllAlerts(Pageable pageable) {
            return alertRepository.findAll(pageable);
        }

        @GetMapping("/unreviewed")
        public Page<Alert> getUnreviewedAlerts(Pageable pageable) {
            return alertRepository.findByReviewedFalse(pageable);
        }

        @GetMapping("/batch/{batchId}")
        public List<Alert> getAlertsByBatch(@PathVariable UUID batchId) {
            return alertRepository.findByBatchId(batchId);
        }

}
