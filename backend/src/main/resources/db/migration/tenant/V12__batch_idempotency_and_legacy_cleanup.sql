-- Add idempotent upload metadata to the active JPA batch table.
ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS batch_date DATE,
    ADD COLUMN IF NOT EXISTS file_checksum CHAR(64);

-- Preserve the date of legacy batches where it can be derived. New uploads always populate all fields.
UPDATE batches
SET batch_date = uploaded_at::date
WHERE batch_date IS NULL AND uploaded_at IS NOT NULL;

-- Partial indexes keep old rows with missing metadata migratable while enforcing new uploads.
CREATE UNIQUE INDEX IF NOT EXISTS uk_batches_tenant_date
    ON batches (tenant_id, batch_date)
    WHERE tenant_id IS NOT NULL AND batch_date IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_batches_tenant_checksum
    ON batches (tenant_id, file_checksum)
    WHERE tenant_id IS NOT NULL AND file_checksum IS NOT NULL;

-- Remove legacy ingestion/ELT artifacts. Active tables are batches, transactions, and aml_alerts.
DROP TABLE IF EXISTS aml_invalid_transactions_dlq CASCADE;
DROP TABLE IF EXISTS aml_transactions_staging CASCADE;
DROP TABLE IF EXISTS transactions_staging CASCADE;
DROP TABLE IF EXISTS aml_showcase_pdfs CASCADE;
DROP TABLE IF EXISTS aml_alert_assignment_items CASCADE;
DROP TABLE IF EXISTS aml_alert_assignments CASCADE;
DROP TABLE IF EXISTS aml_sar_filings CASCADE;
DROP TABLE IF EXISTS aml_case_notes CASCADE;
DROP TABLE IF EXISTS aml_case_alerts CASCADE;
DROP TABLE IF EXISTS aml_cases CASCADE;
DROP TABLE IF EXISTS aml_batches CASCADE;
DROP TABLE IF EXISTS aml_transactions CASCADE;
