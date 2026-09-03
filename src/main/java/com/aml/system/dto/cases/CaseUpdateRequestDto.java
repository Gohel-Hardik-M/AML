package com.aml.system.dto.cases;

import com.aml.system.model.enums.CaseStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdateRequestDto {
    private CaseStatus status;
    @Size(max = 64, message = "Assigned analyst ID must not exceed 64 characters")
    private String assignedAnalystId;
    @Size(max = 5000, message = "Investigation notes must not exceed 5000 characters")
    private String investigationNotes;
    @Size(max = 128, message = "SAR filing reference must not exceed 128 characters")
    private String sarFilingReference;
}
