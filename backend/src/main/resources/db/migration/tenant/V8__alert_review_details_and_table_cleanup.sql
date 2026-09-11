-- =================================================================================
-- V8: Add Alert Review Audit Columns and Clean Up Unused Duplicate Tables
-- =================================================================================

-- 1. Add review tracking columns to aml_alerts for the Compliance Officer workflow
ALTER TABLE aml_alerts ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE aml_alerts ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(128);
ALTER TABLE aml_alerts ADD COLUMN IF NOT EXISTS review_decision VARCHAR(64);
ALTER TABLE aml_alerts ADD COLUMN IF NOT EXISTS review_notes TEXT;

-- 2. Drop unused duplicate / legacy tables from older iterations
-- Active tables retained: aml_alerts, batches, transactions, transactions_staging, aml_tenant_rules, aml_users, aml_system_audit_logs
DROP TABLE IF EXISTS aml_showcase_pdfs CASCADE;
DROP TABLE IF EXISTS aml_alert_assignment_items CASCADE;
DROP TABLE IF EXISTS aml_alert_assignments CASCADE;
DROP TABLE IF EXISTS aml_sar_filings CASCADE;
DROP TABLE IF EXISTS aml_case_notes CASCADE;
DROP TABLE IF EXISTS aml_case_alerts CASCADE;
DROP TABLE IF EXISTS aml_cases CASCADE;
DROP TABLE IF EXISTS aml_invalid_transactions_dlc CASCADE;
DROP TABLE IF EXISTS aml_batches CASCADE;
DROP TABLE IF EXISTS aml_transactions_staging CASCADE;
DROP TABLE IF EXISTS aml_transactions CASCADE;
