-- 3. BATCH & INGESTION PIPELINE

CREATE TABLE IF NOT EXISTS batches (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uploaded_by_id UUID,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL
    );


-- 4. TRANSACTIONS TABLES

CREATE TABLE IF NOT EXISTS transactions_staging (
                                                    transaction_id UUID PRIMARY KEY,
                                                    tenant_id VARCHAR(64) NOT NULL,
    source_account_id VARCHAR(64),
    destination_account_id VARCHAR(64),
    customer_id VARCHAR(64),
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    country_code VARCHAR(10),
    counterparty_country_code VARCHAR(10),
    counterparty_name VARCHAR(255),
    channel VARCHAR(32),
    timestamp TIMESTAMP NOT NULL,
    batch_id UUID NOT NULL REFERENCES batches(id)
    );

CREATE TABLE IF NOT EXISTS transactions (
                                            transaction_id UUID PRIMARY KEY,
                                            tenant_id VARCHAR(64) NOT NULL,
    source_account_id VARCHAR(64),
    destination_account_id VARCHAR(64),
    customer_id VARCHAR(64),
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    country_code VARCHAR(10),
    counterparty_country_code VARCHAR(10),
    counterparty_name VARCHAR(255),
    channel VARCHAR(32),
    timestamp TIMESTAMP NOT NULL,
    batch_id UUID NOT NULL REFERENCES batches(id)
    );

CREATE INDEX IF NOT EXISTS idx_transactions_batch_id ON transactions(batch_id);
CREATE INDEX IF NOT EXISTS idx_transactions_tenant_id ON transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transactions_staging_batch_id ON transactions_staging(batch_id);


-- 5. ELT STORED PROCEDURE
CREATE OR REPLACE PROCEDURE process_batch_transactions(p_batch_id UUID)
LANGUAGE plpgsql
AS $$
DECLARE
v_rows_merged BIGINT;
BEGIN
    RAISE NOTICE 'Starting in-database ELT transformation for Batch ID: %', p_batch_id;

INSERT INTO transactions (
    transaction_id,
    tenant_id,
    source_account_id,
    destination_account_id,
    customer_id,
    amount,
    currency,
    transaction_type,
    country_code,
    counterparty_country_code,
    counterparty_name,
    channel,
    timestamp,
    batch_id
)
SELECT
    s.transaction_id,
    s.tenant_id,
    s.source_account_id,
    s.destination_account_id,
    s.customer_id,
    s.amount,
    s.currency,
    s.transaction_type,
    s.country_code,
    s.counterparty_country_code,
    s.counterparty_name,
    s.channel,
    s.timestamp,
    s.batch_id
FROM transactions_staging s
WHERE s.batch_id = p_batch_id
    ON CONFLICT (transaction_id) DO NOTHING;

GET DIAGNOSTICS v_rows_merged = ROW_COUNT;

RAISE NOTICE 'Merged % records from staging to transactions', v_rows_merged;

DELETE FROM transactions_staging
WHERE batch_id = p_batch_id;
END;
$$;