package com.aml.system.repository;

import com.aml.system.model.Alert;
import com.aml.system.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * =========================================================================================
 * ARCHITECTURAL RULE: STRICT USAGE OF JDBC TEMPLATE FOR 1 CRORE (10M) BATCH WRITES
 * =========================================================================================
 * 
 * WHY JPA / HIBERNATE IS STRICTLY FORBIDDEN FOR HIGH-VOLUME BATCH INGESTION:
 * 1. FIRST-LEVEL CACHE SATURATION (OOM RISK):
 *    Hibernate's PersistenceContext retains managed entity references in JVM Heap. Ingesting
 *    10,000,000 entities via repository.saveAll() forces millions of proxies into Eden/OldGen,
 *    triggering continuous Stop-The-World Major GC pauses and inevitable OutOfMemoryError: Java heap space.
 * 
 * 2. DIRTY-CHECKING OVERHEAD:
 *    Hibernate takes deep state snapshots of every loaded entity to detect mutations upon flush.
 *    For 10M immutable transaction records, snapshot creation wastes massive CPU cycles.
 * 
 * 3. SQL RE-ORDERING & BATCH DISABLING:
 *    Using GenerationType.IDENTITY disables Hibernate batch inserts entirely. Even with GenerationType.SEQUENCE,
 *    Hibernate incurs hydration/reflection penalties.
 * 
 * SOLUTION:
 * - Use Spring's JdbcTemplate.batchUpdate() with parameterized prepared statements.
 * - Set PostgreSQL JDBC connection parameter `reWriteBatchedInserts=true` to combine INSERT statements
 *   into multi-row VALUES clauses (e.g. INSERT INTO txn VALUES (...), (...), (...)) behind the scenes.
 * - Stream chunks of 2,500 - 10,000 records per round-trip to stay within PostgreSQL network packet limits.
 * =========================================================================================
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TransactionBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_INSERT_TRANSACTION_STAGING = """
            INSERT INTO aml_transactions_staging (
                transaction_id, tenant_id, source_account_id, destination_account_id,
                customer_id, amount, currency, transaction_type, country_code,
                counterparty_country_code, counterparty_name, channel, txn_timestamp, batch_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_INSERT_ALERTS_BATCH = """
            INSERT INTO aml_alerts (
                alert_id, tenant_id, transaction_id, customer_id, rule_code,
                rule_name, severity, triggered_amount, narrative, detection_metadata_json,
                is_reviewed, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_INSERT_INVALID_TRANSACTIONS = """
            INSERT INTO aml_invalid_transactions_dlq (
                error_id, batch_id, tenant_id, raw_record, error_reason, failed_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    /**
     * High-speed bulk write for ingested transaction chunks.
     * Zero Hibernate overhead, memory footprint bounded to current chunk size.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int[][] insertTransactionBatch(List<Transaction> transactions, int batchChunkSize) {
        log.debug("Executing JdbcTemplate.batchUpdate for {} transactions with chunk size {}", transactions.size(), batchChunkSize);

        return jdbcTemplate.batchUpdate(
                SQL_INSERT_TRANSACTION_STAGING,
                transactions,
                batchChunkSize,
                (PreparedStatement ps, Transaction txn) -> {
                    ps.setObject(1, txn.getTransactionId());
                    ps.setString(2, txn.getTenantId());
                    ps.setString(3, txn.getSourceAccountId());
                    ps.setString(4, txn.getDestinationAccountId());
                    ps.setString(5, txn.getCustomerId());
                    ps.setBigDecimal(6, txn.getAmount());
                    ps.setString(7, txn.getCurrency());
                    ps.setString(8, txn.getTransactionType() != null ? txn.getTransactionType().name() : null);
                    ps.setString(9, txn.getCountryCode());
                    ps.setString(10, txn.getCounterpartyCountryCode());
                    ps.setString(11, txn.getCounterpartyName());
                    ps.setString(12, txn.getChannel());
                    ps.setTimestamp(13, txn.getTimestamp() != null ? Timestamp.valueOf(txn.getTimestamp()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
                    ps.setObject(14, txn.getBatchId());
                }
        );
    }

    /**
     * High-speed batch persist for compliance alerts generated during rule engine evaluation.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int[][] insertAlertsBatch(List<Alert> alerts, int batchChunkSize) {
        log.debug("Executing JdbcTemplate.batchUpdate for {} generated alerts", alerts.size());

        return jdbcTemplate.batchUpdate(
                SQL_INSERT_ALERTS_BATCH,
                alerts,
                batchChunkSize,
                (PreparedStatement ps, Alert alert) -> {
                    ps.setObject(1, alert.getAlertId() != null ? alert.getAlertId() : UUID.randomUUID());
                    ps.setString(2, alert.getTenantId());
                    ps.setObject(3, alert.getTransactionId());
                    ps.setString(4, alert.getCustomerId());
                    ps.setString(5, alert.getRuleCode());
                    ps.setString(6, alert.getRuleName());
                    ps.setString(7, alert.getSeverity() != null ? alert.getSeverity().name() : "MEDIUM");
                    ps.setBigDecimal(8, alert.getTriggeredAmount());
                    ps.setString(9, alert.getNarrative());
                    ps.setString(10, alert.getDetectionMetadataJson());
                    ps.setBoolean(11, alert.getIsReviewed() != null && alert.getIsReviewed());
                    ps.setTimestamp(12, alert.getCreatedAt() != null ? Timestamp.valueOf(alert.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
                }
        );
    }

    /**
     * Persists Poison-Pill records (malformed CSV/JSON rows) to Dead-Letter Queue (DLQ) table
     * without aborting the active 1 Crore record batch job.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPoisonPill(UUID batchId, String tenantId, String rawRecord, String errorReason) {
        jdbcTemplate.update(
                SQL_INSERT_INVALID_TRANSACTIONS,
                UUID.randomUUID(),
                batchId,
                tenantId,
                rawRecord,
                errorReason,
                new Timestamp(System.currentTimeMillis())
        );
    }
}
