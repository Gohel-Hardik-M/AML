package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.alerts.AlertResponseDto;
import com.aml.system.dto.cases.CaseResponseDto;
import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AlertResponseDto>>> getAlerts(
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) Boolean isReviewed,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AlertResponseDto> alerts = alertService.getAlerts(severity, isReviewed, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts, "Retrieved alerts successfully"));
    }

    @PostMapping("/{alertId}/escalate")
    public ResponseEntity<ApiResponse<CaseResponseDto>> escalateAlert(
            @PathVariable UUID alertId,
            @RequestParam String analystId
    ) {
        CaseResponseDto caseDto = alertService.escalateAlertToCase(alertId, analystId);
        return ResponseEntity.ok(ApiResponse.success(caseDto, "Alert successfully escalated to compliance case"));
    }
}
