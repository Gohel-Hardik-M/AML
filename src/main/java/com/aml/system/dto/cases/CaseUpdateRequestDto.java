package com.aml.system.dto.cases;

import com.aml.system.model.enums.CaseStatus;
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
    private String assignedAnalystId;
    private String investigationNotes;
    private String sarFilingReference;
}
