-- =========================================================================================
-- ISOLATED TENANT SCHEMA & ELT PROCEDURE (Runs per Bank DB)
-- =========================================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. SECURITY, USERS & AUDIT TRAIL
CREATE TABLE IF NOT EXISTS aml_users (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    username VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    role VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_temporary_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT idx_user_tenant_username UNIQUE (tenant_id, username)
    );

CREATE TABLE IF NOT EXISTS aml_system_audit_logs (
                                                     log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128),
    action_type VARCHAR(64) NOT NULL,
    affected_record_id VARCHAR(128),
    ip_address VARCHAR(64),
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 2. BATCH & INGESTION PIPELINE
CREATE TABLE IF NOT EXISTS aml_batches (
                                           batch_id UUID PRIMARY KEY,
                                           file_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_records INTEGER DEFAULT 0,
    uploaded_by VARCHAR(128),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE UNLOGGED TABLE IF NOT EXISTS aml_transactions_staging (
    transaction_id UUID NOT NULL,
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

CREATE TABLE IF NOT EXISTS aml_transactions (
                                                transaction_id UUID NOT NULL,
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

CREATE TABLE IF NOT EXISTS aml_transactions_default PARTITION OF aml_transactions DEFAULT;

CREATE INDEX IF NOT EXISTS idx_txn_customer_time ON aml_transactions (customer_id, txn_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_txn_batch_id ON aml_transactions (batch_id);

CREATE TABLE IF NOT EXISTS aml_invalid_transactions_dlq (
                                                            error_id UUID PRIMARY KEY,
                                                            batch_id UUID NOT NULL,
                                                            raw_record TEXT NOT NULL,
                                                            error_reason TEXT NOT NULL,
                                                            failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. RULES & ALERTS
CREATE TABLE IF NOT EXISTS aml_tenant_rules (
                                                rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    config_thresholds JSONB NOT NULL,
    default_severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS aml_alerts (
                                          alert_id UUID PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_alert_customer ON aml_alerts (customer_id, created_at);

-- 4. CASE MANAGEMENT & SAR FILINGS
CREATE TABLE IF NOT EXISTS aml_cases (
                                         case_id UUID PRIMARY KEY,
                                         customer_id VARCHAR(128) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    assigned_analyst_id VARCHAR(128),
    escalated_to_id VARCHAR(128),
    escalation_reason TEXT,
    primary_rule_triggered VARCHAR(255),
    total_suspicious_amount NUMERIC(19, 4),
    false_positive_rationale TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS aml_case_notes (
                                              note_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES aml_cases(case_id) ON DELETE CASCADE,
    author_id VARCHAR(128) NOT NULL,
    note_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS aml_sar_filings (
                                               filing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL UNIQUE REFERENCES aml_cases(case_id),
    typology_category VARCHAR(128) NOT NULL,
    narrative TEXT NOT NULL,
    pdf_reference VARCHAR(255),
    filed_by VARCHAR(128) NOT NULL,
    filed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 5. ELT STORED PROCEDURE
CREATE OR REPLACE PROCEDURE process_batch_transactions(p_batch_id UUID)
LANGUAGE plpgsql
AS $$
DECLARE
v_rows_merged BIGINT;
BEGIN
    RAISE NOTICE 'Starting in-database ELT transformation for Batch ID: %', p_batch_id;

INSERT INTO aml_transactions (
    transaction_id, source_account_id, destination_account_id,
    customer_id, amount, currency, transaction_type, country_code,
    counterparty_country_code, counterparty_name, channel, txn_timestamp, batch_id
)
SELECT
    s.transaction_id, s.source_account_id, s.destination_account_id,
    s.customer_id, s.amount, s.currency, s.transaction_type, s.country_code,
    s.counterparty_country_code, s.counterparty_name, s.channel, s.txn_timestamp, s.batch_id
FROM aml_transactions_staging s
WHERE s.batch_id = p_batch_id
    ON CONFLICT (transaction_id, txn_timestamp) DO NOTHING;

GET DIAGNOSTICS v_rows_merged = ROW_COUNT;
RAISE NOTICE 'Merged % records from staging to aml_transactions', v_rows_merged;

DELETE FROM aml_transactions_staging WHERE batch_id = p_batch_id;
END;
$$;