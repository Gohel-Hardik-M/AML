package com.aml.system.model;

import com.aml.system.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * High-Throughput Transaction POJO / Entity.
 *
 * ARCHITECTURAL NOTE (JPA vs JDBC vs STAGING):
 * At 1 Crore (10M) records/batch, transactions should NOT be mapped as full JPA entities
 * with Hibernate dirty checking or cascade graph traversal during batch ingestion.
 * They are ingested into PostgreSQL unlogged staging tables via COPY / JdbcTemplate,
 * processed via Spring Batch / ELT Stored Procedures, and streamed efficiently.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private UUID transactionId;
    private String tenantId;
    private String sourceAccountId;
    private String destinationAccountId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private TransactionType transactionType;
    private String countryCode;
    private String counterpartyCountryCode;
    private String counterpartyName;
    private String channel;
    private LocalDateTime timestamp;
    private UUID batchId;
}
