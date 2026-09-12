package com.aml.system.service;

import com.aml.system.dto.admin.AlertAssignmentDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.Alert;
import com.aml.system.model.UserEntity;
import com.aml.system.model.UserRole;
import com.aml.system.multitenancy.TenantContextHolder;
import com.aml.system.repository.AlertRepository;
import com.aml.system.model.Batch;
import com.aml.system.repository.BatchRepository;
import com.aml.system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for Bank Admin to assign/unassign alerts to Compliance Officers,
 * and for Compliance Officers to view their assigned workload.
 */
@Slf4j
@Service
public class AlertAssignmentService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final AlertPdfService alertPdfService;

    public AlertAssignmentService(AlertRepository alertRepository, UserRepository userRepository, BatchRepository batchRepository,
                                  AlertPdfService alertPdfService) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.alertPdfService = alertPdfService;
    }

    /**
     * Assigns one or more alerts to a specific active compliance officer.
     */
    @Transactional
    public String assignAlerts(AlertAssignmentDto dto) {
        String tenantId = TenantContextHolder.getTenantId();

        // 1. Verify officer exists, belongs to tenant, is active, and has COMPLIANCE_OFFICER role
        UserEntity officer = userRepository.findByTenantIdAndUserId(tenantId, dto.getOfficerId())
                .orElseThrow(() -> new AmlBusinessException("Compliance officer not found in this bank.", HttpStatus.NOT_FOUND));

        if (officer.getRole() != UserRole.COMPLIANCE_OFFICER) {
            throw new AmlBusinessException("Selected user is not a compliance officer.", HttpStatus.BAD_REQUEST);
        }

        if (Boolean.FALSE.equals(officer.getIsActive())) {
            throw new AmlBusinessException("Cannot assign alerts to a deactivated compliance officer.", HttpStatus.BAD_REQUEST);
        }

        // 2. Assign each alert to the officer
        int assignedCount = 0;
        for (UUID alertId : dto.getAlertIds()) {
            Alert alert = alertRepository.findById(alertId)
                    .orElseThrow(() -> new AmlBusinessException(
                            "Alert '" + alertId + "' was not found.", HttpStatus.NOT_FOUND));
            if (alert.isReviewed()) {
                throw new AmlBusinessException("Alert '" + alertId + "' is already closed.", HttpStatus.CONFLICT);
            }
            if (alert.getAssignedOfficerId() != null) {
                throw new AmlBusinessException("Alert '" + alertId + "' is already assigned to another compliance officer.", HttpStatus.CONFLICT);
            }
            alert.setAssignedOfficerId(officer.getUserId());
            alertRepository.save(alert);
            assignedCount++;
        }

        log.info("Assigned {} alert(s) to officer '{}' ({}) in tenant '{}'",
                assignedCount, officer.getUsername(), officer.getUserId(), tenantId);

        return "Successfully assigned " + assignedCount + " alert(s) to officer " + officer.getFullName() + ".";
    }

    @Transactional
    public String unassignAlerts(List<UUID> alertIds) {
        if (alertIds == null || alertIds.isEmpty()) {
            throw new AmlBusinessException("No alert IDs provided to unassign.", HttpStatus.BAD_REQUEST);
        }

        int unassignedCount = 0;
        for (UUID alertId : alertIds) {
            Alert alert = alertRepository.findById(alertId).orElse(null);
            if (alert != null && alert.getAssignedOfficerId() != null) {
                alert.setAssignedOfficerId(null);
                alertRepository.save(alert);
                unassignedCount++;
            }
        }

        return "Successfully unassigned " + unassignedCount + " alert(s).";
    }

    /**
     * Returns all alerts that have not yet been assigned to any officer.
     */
    @Transactional(readOnly = true)
    public List<Alert> getUnassignedAlerts() {
        return alertRepository.findByAssignedOfficerIdIsNull();
    }

    /**
     * Returns all alerts assigned to a specific compliance officer.
     */
    @Transactional(readOnly = true)
    public List<Alert> getAlertsByOfficer(UUID officerId) {
        String tenantId = TenantContextHolder.getTenantId();
        // Verify officer belongs to this bank
        userRepository.findByTenantIdAndUserId(tenantId, officerId)
                .orElseThrow(() -> new AmlBusinessException("Compliance officer not found.", HttpStatus.NOT_FOUND));

        return alertRepository.findByAssignedOfficerId(officerId);
    }

    /**
     * For Compliance Officer self-service: returns alerts assigned to the current logged-in user.
     */
    @Transactional(readOnly = true)
    public Page<Alert> getMyAlerts(String username, Pageable pageable) {
        String tenantId = TenantContextHolder.getTenantId();
        UserEntity user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new AmlBusinessException("User account not found.", HttpStatus.NOT_FOUND));

        return alertRepository.findByAssignedOfficerId(user.getUserId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Alert> getMyBatchAlerts(UUID batchId, String username) {
        String tenantId = TenantContextHolder.getTenantId();
        UserEntity user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new AmlBusinessException("User account not found.", HttpStatus.NOT_FOUND));
        return alertRepository.findByBatchIdAndAssignedOfficerId(batchId, user.getUserId());
    }

    @Transactional(readOnly = true)
    public List<UUID> getMyBatchIds(String username) {
        String tenantId = TenantContextHolder.getTenantId();
        UserEntity user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new AmlBusinessException("User account not found.", HttpStatus.NOT_FOUND));
        return alertRepository.findDistinctBatchIdByAssignedOfficerId(user.getUserId());
    }

    @Transactional(readOnly = true)
    public Alert getMyAlert(UUID alertId, String username) {
        String tenantId = TenantContextHolder.getTenantId();
        UserEntity user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new AmlBusinessException("User account not found.", HttpStatus.NOT_FOUND));
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AmlBusinessException("Alert '" + alertId + "' was not found.", HttpStatus.NOT_FOUND));
        if (!user.getUserId().equals(alert.getAssignedOfficerId())) {
            throw new AmlBusinessException("Alert is not assigned to the current compliance officer.", HttpStatus.FORBIDDEN);
        }
        return alert;
    }

    @Transactional
    public Alert closeMyAlert(UUID alertId, String username, String reviewNotes) {
        Alert alert = getMyAlert(alertId, username);
        validateClosure(alert, alertId, reviewNotes);
        applyClosure(alert, username, reviewNotes);
        return saveClosedAlert(alert);
    }

    @Transactional
    public PdfResult closeMyAlertAndGeneratePdf(UUID alertId, String username, String reviewNotes) throws java.io.IOException {
        Alert alert = getMyAlert(alertId, username);
        validateClosure(alert, alertId, reviewNotes);
        Alert closurePreview = closurePreview(alert, username, reviewNotes);

        // Build the document before the managed entity is flushed. If generation fails,
        // the surrounding transaction exits without saving the closure.
        byte[] pdf = alertPdfService.generate(closurePreview);
        applyClosure(alert, username, reviewNotes);
        return new PdfResult(saveClosedAlert(alert), pdf);
    }

    private Alert closurePreview(Alert alert, String username, String reviewNotes) {
        return Alert.builder()
                .alertId(alert.getAlertId())
                .transactionId(alert.getTransactionId())
                .customerId(alert.getCustomerId())
                .ruleCode(alert.getRuleCode())
                .ruleName(alert.getRuleName())
                .severity(alert.getSeverity())
                .triggeredAmount(alert.getTriggeredAmount())
                .narrative(alert.getNarrative())
                .detectionMetadataJson(alert.getDetectionMetadataJson())
                .reviewed(true)
                .assignedCaseId(alert.getAssignedCaseId())
                .batchId(alert.getBatchId())
                .tenantId(alert.getTenantId())
                .assignedOfficerId(alert.getAssignedOfficerId())
                .reviewedAt(LocalDateTime.now())
                .reviewedBy(username)
                .reviewDecision("CLOSED")
                .reviewNotes(reviewNotes.trim())
                .createdAt(alert.getCreatedAt())
                .build();
    }

    private void validateClosure(Alert alert, UUID alertId, String reviewNotes) {
        if (alert.isReviewed()) {
            throw new AmlBusinessException("Alert '" + alertId + "' is already closed.", HttpStatus.CONFLICT);
        }
        if (reviewNotes == null || reviewNotes.isBlank()) {
            throw new AmlBusinessException("A review note is required to close an alert.", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyClosure(Alert alert, String username, String reviewNotes) {
        alert.setReviewed(true);
        alert.setReviewedAt(LocalDateTime.now());
        alert.setReviewedBy(username);
        alert.setReviewDecision("CLOSED");
        alert.setReviewNotes(reviewNotes.trim());
    }

    private Alert saveClosedAlert(Alert alert) {
        Alert saved = alertRepository.save(alert);
        if (saved.getBatchId() != null && alertRepository.countByBatchIdAndReviewedFalse(saved.getBatchId()) == 0) {
            batchRepository.findById(saved.getBatchId()).ifPresent(batch -> {
                batch.setStatus(com.aml.system.model.BatchStatus.REVIEWED);
                batchRepository.save(batch);
            });
        }
        return saved;
    }

    public record PdfResult(Alert alert, byte[] content) { }
}
