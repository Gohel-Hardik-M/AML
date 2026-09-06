-- =================================================================================
-- V3: Final Batch, Alert Assignment, and PDF Showcase Flow
-- Ensures safe migration over V1 and V2 without relying on CREATE TABLE IF NOT EXISTS
-- =================================================================================

-- 1. MUTATE BATCH TABLES
ALTER TABLE aml_batches
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS batch_date DATE,
    ADD COLUMN IF NOT EXISTS file_checksum CHAR(64),
    ADD COLUMN IF NOT EXISTS channel VARCHAR(32),
    ADD COLUMN IF NOT EXISTS valid_records INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS invalid_records INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS alerts_generated INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_aml_batches_tenant_date') THEN
ALTER TABLE aml_batches ADD CONSTRAINT uk_aml_batches_tenant_date UNIQUE (tenant_id, batch_date);
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_aml_batches_tenant_checksum') THEN
ALTER TABLE aml_batches ADD CONSTRAINT uk_aml_batches_tenant_checksum UNIQUE (tenant_id, file_checksum);
END IF;
END $$;

ALTER TABLE aml_invalid_transactions_dlq
    ADD COLUMN IF NOT EXISTS row_number INTEGER;

-- 2. RECREATE TRANSACTIONS TABLE
-- (Must be recreated to remove V1 partitioning and change to single UUID Primary Key)
DROP TABLE IF EXISTS aml_transactions CASCADE;

CREATE TABLE aml_transactions (
                                  transaction_id UUID PRIMARY KEY,
                                  tenant_id VARCHAR(64) NOT NULL,
                                  batch_id UUID NOT NULL,
                                  source_account_id VARCHAR(128),
                                  destination_account_id VARCHAR(128),
                                  customer_id VARCHAR(128) NOT NULL,
                                  amount NUMERIC(19,4) NOT NULL,
                                  currency VARCHAR(10) NOT NULL,
                                  transaction_type VARCHAR(64),
                                  country_code VARCHAR(10),
                                  counterparty_country_code VARCHAR(10),
                                  counterparty_name VARCHAR(255),
                                  channel VARCHAR(64),
                                  txn_timestamp TIMESTAMP NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (batch_id) REFERENCES aml_batches(batch_id),
                                  UNIQUE (tenant_id, transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_transaction_customer_time ON aml_transactions (tenant_id, customer_id, txn_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_batch ON aml_transactions (tenant_id, batch_id);

-- 3. MUTATE ALERTS TABLE
ALTER TABLE aml_alerts
    ADD COLUMN IF NOT EXISTS batch_id UUID;

CREATE INDEX IF NOT EXISTS idx_alert_customer_time ON aml_alerts (tenant_id, customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_batch ON aml_alerts (tenant_id, batch_id);

-- 4. NEW ASSIGNMENT TABLES
CREATE TABLE IF NOT EXISTS aml_alert_assignments (
                                                     assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    assigned_by UUID NOT NULL,
    assigned_to UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS aml_alert_assignment_items (
                                                          assignment_id UUID NOT NULL,
                                                          alert_id UUID NOT NULL,
                                                          customer_id VARCHAR(128) NOT NULL,
    PRIMARY KEY (assignment_id, alert_id),
    FOREIGN KEY (assignment_id) REFERENCES aml_alert_assignments(assignment_id) ON DELETE CASCADE,
    FOREIGN KEY (alert_id) REFERENCES aml_alerts(alert_id)
    );

-- 5. NEW PDF SHOWCASE TABLES
CREATE TABLE IF NOT EXISTS aml_showcase_pdfs (
                                                 pdf_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    generated_by UUID NOT NULL,
    storage_uri VARCHAR(1024) NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, content_sha256)
    );

-- 6. MUTATE CASES TABLE
ALTER TABLE aml_cases
    ADD COLUMN IF NOT EXISTS pdf_id UUID,
    ADD COLUMN IF NOT EXISTS created_by UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_aml_cases_case_customer') THEN
ALTER TABLE aml_cases ADD CONSTRAINT uk_aml_cases_case_customer UNIQUE (case_id, customer_id);
END IF;
END $$;

CREATE TABLE IF NOT EXISTS aml_case_alerts (
                                               case_id UUID NOT NULL,
                                               alert_id UUID NOT NULL,
                                               customer_id VARCHAR(128) NOT NULL,
    PRIMARY KEY (case_id, alert_id),
    FOREIGN KEY (case_id) REFERENCES aml_cases(case_id) ON DELETE CASCADE,
    FOREIGN KEY (alert_id) REFERENCES aml_alerts(alert_id)
    );

CREATE INDEX IF NOT EXISTS idx_pdf_customer_time ON aml_showcase_pdfs (tenant_id, customer_id, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_case_customer_time ON aml_cases (tenant_id, customer_id, created_at DESC);

-- 7. REFRESH ELT PROCEDURE
CREATE OR REPLACE PROCEDURE process_batch_transactions(p_batch_id UUID)
LANGUAGE plpgsql
AS $proc$
DECLARE
v_rows_merged BIGINT;
BEGIN
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
    ON CONFLICT (tenant_id, transaction_id) DO NOTHING;

GET DIAGNOSTICS v_rows_merged = ROW_COUNT;
RAISE NOTICE 'Merged % records from staging to aml_transactions', v_rows_merged;

DELETE FROM aml_transactions_staging WHERE batch_id = p_batch_id;
END;
$proc$;