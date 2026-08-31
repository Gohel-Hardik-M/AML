-- =========================================================================================
-- HIGH-SCALE MULTI-TENANT POSTGRESQL AML SCHEMA & ELT STORED PROCEDURE (10M SCALE)
-- =========================================================================================

-- 1. UNLOGGED STAGING TABLE FOR MAXIMUM BULK INGESTION SPEED (Zero WAL overhead for staging)
CREATE UNLOGGED TABLE IF NOT EXISTS aml_transactions_staging (
    transaction_id UUID NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    source_account_id VARCHAR(128),
    destination_account_id VARCHAR(128),
    customer_id VARCHAR(128) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(64),
    country_code VARCHAR(10),
    counterparty_country_code VARCHAR(10),
    counterparty_name VARCHAR(255),
    channel VARCHAR(64),
    txn_timestamp TIMESTAMP NOT NULL,
    batch_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_staging_batch_customer ON aml_transactions_staging (batch_id, customer_id);

-- 2. PARTITIONED CORE TRANSACTIONS TABLE (Range Partitioned by Month)
CREATE TABLE IF NOT EXISTS aml_transactions (
    transaction_id UUID NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    source_account_id VARCHAR(128),
    destination_account_id VARCHAR(128),
    customer_id VARCHAR(128) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(64),
    country_code VARCHAR(10),
    counterparty_country_code VARCHAR(10),
    counterparty_name VARCHAR(255),
    channel VARCHAR(64),
    txn_timestamp TIMESTAMP NOT NULL,
    batch_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id, txn_timestamp)
) PARTITION BY RANGE (txn_timestamp);

-- 3. ALERTS TABLE
CREATE TABLE IF NOT EXISTS aml_alerts (
    alert_id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    transaction_id UUID NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    triggered_amount NUMERIC(19, 4) NOT NULL,
    narrative TEXT,
    detection_metadata_json TEXT,
    is_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_case_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_tenant_rule ON aml_alerts (tenant_id, rule_code);
CREATE INDEX IF NOT EXISTS idx_alert_customer ON aml_alerts (customer_id, created_at);

-- 4. CASES TABLE
CREATE TABLE IF NOT EXISTS aml_cases (
    case_id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    assigned_analyst_id VARCHAR(128),
    primary_rule_triggered VARCHAR(255),
    total_suspicious_amount NUMERIC(19, 4),
    investigation_notes TEXT,
    sar_filing_reference VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. DEAD-LETTER QUEUE (DLQ) TABLE FOR POISON PILL RECORDS
CREATE TABLE IF NOT EXISTS aml_invalid_transactions_dlq (
    error_id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    raw_record TEXT NOT NULL,
    error_reason TEXT NOT NULL,
    failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. STORED PROCEDURE FOR IN-DATABASE HIGH-PERFORMANCE ELT PROCESSING
-- Executes parallel SQL transformations directly inside PostgreSQL
CREATE OR REPLACE PROCEDURE process_batch_transactions(p_batch_id UUID)
LANGUAGE plpgsql
AS $$
DECLARE
    v_rows_merged BIGINT;
BEGIN
    RAISE NOTICE 'Starting in-database ELT transformation for Batch ID: %', p_batch_id;

    -- 1. Atomic Merge/Insert from Unlogged Staging into Core Partitioned Table
    INSERT INTO aml_transactions (
        transaction_id, tenant_id, source_account_id, destination_account_id,
        customer_id, amount, currency, transaction_type, country_code,
        counterparty_country_code, counterparty_name, channel, txn_timestamp, batch_id
    )
    SELECT
        s.transaction_id, s.tenant_id, s.source_account_id, s.destination_account_id,
        s.customer_id, s.amount, s.currency, s.transaction_type, s.country_code,
        s.counterparty_country_code, s.counterparty_name, s.channel, s.txn_timestamp, s.batch_id
    FROM aml_transactions_staging s
    WHERE s.batch_id = p_batch_id
    ON CONFLICT (transaction_id, txn_timestamp) DO NOTHING;

    GET DIAGNOSTICS v_rows_merged = ROW_COUNT;
    RAISE NOTICE 'Merged % records from staging to aml_transactions', v_rows_merged;

    -- 2. Cleanup staging partition for this batch
    DELETE FROM aml_transactions_staging WHERE batch_id = p_batch_id;

    RAISE NOTICE 'Batch % ELT Stored Procedure successfully completed.', p_batch_id;
END;
$$;
