package com.aml.system.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO for assigning or unassigning alerts to/from a Compliance Officer.
 * Bank Admin sends a list of alert IDs and the officer ID.
 */
@Data
public class AlertAssignmentDto {

    @NotEmpty(message = "At least one alert ID is required")
    private List<UUID> alertIds;

    @NotNull(message = "Officer ID is required")
    private UUID officerId;
}
