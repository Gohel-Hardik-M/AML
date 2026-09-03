package com.aml.system.controller;

import com.aml.system.dto.ApiResponse;
import com.aml.system.dto.cases.CaseResponseDto;
import com.aml.system.dto.cases.CaseUpdateRequestDto;
import com.aml.system.model.enums.CaseStatus;
import com.aml.system.service.CaseService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CaseResponseDto>>> getCases(
            @RequestParam(required = false) CaseStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<CaseResponseDto> cases = caseService.getCases(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(cases, "Retrieved compliance cases"));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CaseResponseDto>> getCaseById(@PathVariable UUID caseId) {
        CaseResponseDto caseDto = caseService.getCaseById(caseId);
        return ResponseEntity.ok(ApiResponse.success(caseDto, "Retrieved case details"));
    }

    @PatchMapping("/{caseId}")
    public ResponseEntity<ApiResponse<CaseResponseDto>> updateCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody CaseUpdateRequestDto updateRequest
    ) {
        CaseResponseDto caseDto = caseService.updateCase(caseId, updateRequest);
        return ResponseEntity.ok(ApiResponse.success(caseDto, "Case updated successfully"));
    }
}
