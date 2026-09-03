ALTER TABLE IF EXISTS aml_transactions_staging
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);

ALTER TABLE IF EXISTS aml_transactions
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);

ALTER TABLE IF EXISTS aml_invalid_transactions_dlq
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);

ALTER TABLE IF EXISTS aml_alerts
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);

ALTER TABLE IF EXISTS aml_cases
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_staging_tenant_batch
    ON aml_transactions_staging (tenant_id, batch_id);

CREATE INDEX IF NOT EXISTS idx_transaction_tenant_customer
    ON aml_transactions (tenant_id, customer_id, txn_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_alert_tenant_reviewed
    ON aml_alerts (tenant_id, is_reviewed);

CREATE INDEX IF NOT EXISTS idx_case_tenant_status
    ON aml_cases (tenant_id, status);

CREATE OR REPLACE PROCEDURE process_batch_transactions(p_batch_id UUID)
LANGUAGE plpgsql
AS $$
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
    ON CONFLICT (transaction_id, txn_timestamp) DO NOTHING;

    GET DIAGNOSTICS v_rows_merged = ROW_COUNT;
    RAISE NOTICE 'Merged % records from staging to aml_transactions', v_rows_merged;

    DELETE FROM aml_transactions_staging WHERE batch_id = p_batch_id;
END;
$$;
