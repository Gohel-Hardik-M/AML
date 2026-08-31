package com.aml.system.exception;

import lombok.Getter;

/**
 * Standardized AML System Error Codes.
 * Used across the system for predictable REST responses and automated incident categorization.
 */
@Getter
public enum ErrorCodeEnum {

    // 1000 - Tenant & Authentication Failures
    TENANT_NOT_FOUND("AML_1001", "Tenant identifier not found or inactive", 404),
    TENANT_ROUTING_FAILED("AML_1002", "Failed to resolve database connection pool for tenant", 500),
    UNAUTHORIZED_ACCESS("AML_1003", "Authentication required or invalid token", 401),
    FORBIDDEN_OPERATION("AML_1004", "Insufficient compliance clearance for operation", 403),

    // 2000 - Ingestion & Batch Processing
    BATCH_FILE_CORRUPTED("AML_2001", "Uploaded batch file is malformed or unreadable", 400),
    BATCH_POISON_PILL("AML_2002", "Unparseable poison pill record encountered during chunk processing", 422),
    BATCH_EXECUTION_TIMEOUT("AML_2003", "Batch job exceeded maximum SLA threshold", 504),
    BATCH_DUPLICATE_INGESTION("AML_2004", "Batch checksum indicates file was previously ingested", 409),
    BATCH_ELT_PROCEDURE_FAILED("AML_2005", "PostgreSQL ELT Stored Procedure execution failed", 500),

    // 3000 - Rule Engine & DSA Violations
    RULE_EVALUATION_ERROR("AML_3001", "Critical failure during rule evaluation pipeline", 500),
    RULE_CONFIG_NOT_FOUND("AML_3002", "Tenant-specific rule threshold configuration missing", 400),
    SLIDING_WINDOW_BUFFER_OVERFLOW("AML_3003", "Transaction history sequence exceeded max allocated buffer size", 500),

    // 4000 - Case & Alert Management
    CASE_NOT_FOUND("AML_4001", "Requested compliance case does not exist", 404),
    ALERT_NOT_FOUND("AML_4002", "Requested AML alert does not exist", 404),
    INVALID_CASE_TRANSITION("AML_4003", "Illegal workflow state transition for compliance case", 400),

    // 5000 - System & Infrastructure
    INTERNAL_SYSTEM_ERROR("AML_5001", "An unexpected internal server error occurred", 500),
    DATABASE_DEADLOCK_DETECTED("AML_5002", "Database transaction deadlock occurred during high concurrency update", 500);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    ErrorCodeEnum(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
