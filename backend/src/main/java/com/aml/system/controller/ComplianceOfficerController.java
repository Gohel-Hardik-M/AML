package com.aml.system.controller;

import com.aml.system.dto.admin.ComplianceOfficerCreateDto;
import com.aml.system.dto.admin.ComplianceOfficerResponseDto;
import com.aml.system.service.ComplianceOfficerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank-admin/compliance-officers")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class ComplianceOfficerController {

    private final ComplianceOfficerService officerService;

    public ComplianceOfficerController(ComplianceOfficerService officerService) {
        this.officerService = officerService;
    }

    @PostMapping
    public ResponseEntity<ComplianceOfficerResponseDto> createOfficer(
            @Valid @RequestBody ComplianceOfficerCreateDto request
    ) {
        ComplianceOfficerResponseDto officer = officerService.createComplianceOfficer(request);
        return ResponseEntity.ok(officer);
    }

    @GetMapping
    public ResponseEntity<List<ComplianceOfficerResponseDto>> getOfficers() {
        List<ComplianceOfficerResponseDto> officers = officerService.getComplianceOfficers();
        return ResponseEntity.ok(officers);
    }

    @PutMapping("/{officerId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateOfficer(@PathVariable UUID officerId) {
        String message = officerService.deactivateComplianceOfficer(officerId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PutMapping("/{officerId}/reactivate")
    public ResponseEntity<Map<String, String>> reactivateOfficer(@PathVariable UUID officerId) {
        String message = officerService.reactivateComplianceOfficer(officerId);
        return ResponseEntity.ok(Map.of("message", message));
    }
}
