package com.aml.system.controller;

import com.aml.system.dto.admin.AlertAssignmentDto;
import com.aml.system.dto.compliance.AlertReviewRequestDto;
import com.aml.system.model.Alert;
import com.aml.system.service.AlertAssignmentService;
import com.aml.system.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class AlertAssignmentController {

    private final AlertAssignmentService alertAssignmentService;
    private final AuditLogService auditLogService;

    public AlertAssignmentController(AlertAssignmentService alertAssignmentService, AuditLogService auditLogService) {
        this.alertAssignmentService = alertAssignmentService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/v1/bank-admin/alerts/assign")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, String>> assignAlerts(@Valid @RequestBody AlertAssignmentDto dto, Principal principal, HttpServletRequest request) {
        String result = alertAssignmentService.assignAlerts(dto);
        auditLogService.logAction(principal.getName(), "ALERTS_ASSIGNED", dto.getOfficerId().toString(), "Assigned " + dto.getAlertIds().size() + " alert(s)", request);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/api/v1/bank-admin/alerts/unassign")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, String>> unassignAlerts(@RequestBody List<UUID> alertIds, Principal principal, HttpServletRequest request) {
        String result = alertAssignmentService.unassignAlerts(alertIds);
        auditLogService.logAction(principal.getName(), "ALERTS_UNASSIGNED", null, "Unassigned " + alertIds.size() + " alert(s)", request);
        return ResponseEntity.ok(Map.of("message", result));
    }

    /**
     * Bank Admin views all unassigned alerts.
     */
    @GetMapping("/api/v1/bank-admin/alerts/unassigned")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<List<Alert>> getUnassignedAlerts() {
        List<Alert> alerts = alertAssignmentService.getUnassignedAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Bank Admin views alerts assigned to a specific compliance officer.
     */
    @GetMapping("/api/v1/bank-admin/alerts/officer/{officerId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<List<Alert>> getAlertsByOfficer(@PathVariable UUID officerId) {
        List<Alert> alerts = alertAssignmentService.getAlertsByOfficer(officerId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Compliance Officer views alerts assigned to themselves.
     */
    @GetMapping("/api/v1/compliance/alerts/my-alerts")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<Alert>> getMyAlerts(Principal principal, Pageable pageable) {
        String username = principal.getName();
        Page<Alert> alerts = alertAssignmentService.getMyAlerts(username, pageable);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/api/v1/compliance/alerts/batch/{batchId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<Alert>> getMyBatchAlerts(@PathVariable UUID batchId, Principal principal) {
        return ResponseEntity.ok(alertAssignmentService.getMyBatchAlerts(batchId, principal.getName()));
    }

    @GetMapping("/api/v1/compliance/alerts/my-batches")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<UUID>> getMyBatchIds(Principal principal) {
        return ResponseEntity.ok(alertAssignmentService.getMyBatchIds(principal.getName()));
    }

    @GetMapping("/api/v1/compliance/alerts/{alertId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Alert> getMyAlert(
            @PathVariable UUID alertId,
            Principal principal
    ) {
        return ResponseEntity.ok(alertAssignmentService.getMyAlert(alertId, principal.getName()));
    }

    @PutMapping("/api/v1/compliance/alerts/{alertId}/close")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Alert> closeMyAlert(
            @PathVariable UUID alertId,
            Principal principal, HttpServletRequest request,
            @Valid @RequestBody AlertReviewRequestDto reviewRequest
    ) {
        Alert closed = alertAssignmentService.closeMyAlert(alertId, principal.getName(), reviewRequest.getReviewNotes());
        auditLogService.logAction(principal.getName(), "ALERT_CLOSED", alertId.toString(), "Compliance Officer closed alert", request);
        return ResponseEntity.ok(closed);
    }

    @PostMapping(value = "/api/v1/compliance/alerts/{alertId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> generateAlertPdf(@PathVariable UUID alertId, Principal principal, HttpServletRequest request,
                                                   @Valid @RequestBody AlertReviewRequestDto reviewRequest) throws java.io.IOException {
        AlertAssignmentService.PdfResult result = alertAssignmentService.closeMyAlertAndGeneratePdf(
            alertId, principal.getName(), reviewRequest.getReviewNotes());
        auditLogService.logAction(principal.getName(), "ALERT_CLOSED_PDF_GENERATED", alertId.toString(), "Alert closed and PDF generated", request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=aml-alert-" + alertId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
            .body(result.content());
    }
}
