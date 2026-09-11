package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.admin.ComplianceOfficerCreateDto;
import com.aml.system.dto.admin.ComplianceOfficerResponseDto;
import com.aml.system.service.ComplianceOfficerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<ApiResponse<ComplianceOfficerResponseDto>> createOfficer(
            @Valid @RequestBody ComplianceOfficerCreateDto request
    ) {
        ComplianceOfficerResponseDto officer = officerService.createComplianceOfficer(request);
        return ResponseEntity.ok(ApiResponse.success(officer, "Compliance officer created successfully. Login credentials emailed."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComplianceOfficerResponseDto>>> getOfficers() {
        List<ComplianceOfficerResponseDto> officers = officerService.getComplianceOfficers();
        return ResponseEntity.ok(ApiResponse.success(officers));
    }


    @PutMapping("/{officerId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateOfficer(@PathVariable UUID officerId) {
        String message = officerService.deactivateComplianceOfficer(officerId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Reactivate a compliance officer.
     */
    @PutMapping("/{officerId}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateOfficer(@PathVariable UUID officerId) {
        String message = officerService.reactivateComplianceOfficer(officerId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
