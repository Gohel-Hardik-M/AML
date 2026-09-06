package com.aml.system.dto.batch;

import lombok.Data;

/**
 * Represents a single row read from the uploaded CSV/Excel file.
 */
@Data
public class TransactionCsvInput {
    // 1. Fields directly mapped from the CSV columns
    private String transactionId;
    private String sourceAccountId;
    private String destinationAccountId;
    private String customerId;
    private Double amount;
    private String currency;
    private String transactionType;
    private String countryCode;
    private String counterpartyCountryCode;
    private String counterpartyName;
    private String channel;
    private String txnTimestamp;

    // 2. Context fields injected by the TransactionItemProcessor
    private String tenantId;
    private String batchId;
}