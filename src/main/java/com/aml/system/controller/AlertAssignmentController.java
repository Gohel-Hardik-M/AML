package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.admin.AlertAssignmentDto;
import com.aml.system.model.Alert;
import com.aml.system.service.AlertAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;


@RestController
public class AlertAssignmentController {

    private final AlertAssignmentService alertAssignmentService;

    public AlertAssignmentController(AlertAssignmentService alertAssignmentService) {
        this.alertAssignmentService = alertAssignmentService;
    }

    @PostMapping("/api/v1/bank-admin/alerts/assign")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignAlerts(@Valid @RequestBody AlertAssignmentDto dto) {
        String result = alertAssignmentService.assignAlerts(dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/api/v1/bank-admin/alerts/unassign")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unassignAlerts(@RequestBody List<UUID> alertIds) {
        String result = alertAssignmentService.unassignAlerts(alertIds);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Bank Admin views all unassigned alerts.
     */
    @GetMapping("/api/v1/bank-admin/alerts/unassigned")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<List<Alert>>> getUnassignedAlerts() {
        List<Alert> alerts = alertAssignmentService.getUnassignedAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Bank Admin views alerts assigned to a specific compliance officer.
     */
    @GetMapping("/api/v1/bank-admin/alerts/officer/{officerId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlertsByOfficer(@PathVariable UUID officerId) {
        List<Alert> alerts = alertAssignmentService.getAlertsByOfficer(officerId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Compliance Officer views alerts assigned to themselves.
     */
    @GetMapping("/api/v1/compliance/alerts/my-alerts")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<Alert>>> getMyAlerts(Principal principal) {
        String username = principal.getName();
        List<Alert> alerts = alertAssignmentService.getMyAlerts(username);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/api/v1/compliance/alerts/{alertId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Alert>> getMyAlert(
            @PathVariable UUID alertId,
            Principal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                alertAssignmentService.getMyAlert(alertId, principal.getName())));
    }

    @PutMapping("/api/v1/compliance/alerts/{alertId}/close")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Alert>> closeMyAlert(
            @PathVariable UUID alertId,
            Principal principal
    ) {
        Alert closed = alertAssignmentService.closeMyAlert(alertId, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(closed, "Alert closed successfully."));
    }
}
