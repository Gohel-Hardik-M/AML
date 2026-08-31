package com.aml.system.model.enums;

public enum CaseStatus {
    OPEN,
    UNDER_INVESTIGATION,
    ESCALATED_TO_L2,
    SAR_FILED,          // Suspicious Activity Report filed with FIU/FinCEN
    CLOSED_FALSE_POSITIVE,
    CLOSED_RESOLVED
}
