package com.aml.system.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TenantRuleAllocationDto {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotEmpty(message = "At least one rule code is required")
    private List<String> ruleCodes;
}
