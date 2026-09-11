package com.aml.system.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for Bank Admin to update rule configuration values.
 * Only the fields that are NOT null will be updated.
 */
@Data
public class RuleConfigUpdateDto {

    // The money threshold for this rule (must be strictly greater than 0)
    @DecimalMin(value = "0.01", message = "Threshold amount must be greater than 0")
    private BigDecimal thresholdAmount;

    // Time window in minutes (must be strictly positive)
    @Positive(message = "Window minutes must be greater than 0")
    private Integer windowMinutes;

    // Maximum count (must be strictly positive)
    @Positive(message = "Max count must be greater than 0")
    private Integer maxCount;

    // Whether this rule is turned on or off
    private Boolean isEnabled;

    // Optional percentage deviation threshold (0 to 100)
    @DecimalMin(value = "0.00", message = "Percentage deviation must be >= 0")
    @DecimalMax(value = "100.00", message = "Percentage deviation cannot exceed 100")
    private BigDecimal percentageDeviation;

    // Optional custom parameters as JSON string
    private String customParametersJson;
}
