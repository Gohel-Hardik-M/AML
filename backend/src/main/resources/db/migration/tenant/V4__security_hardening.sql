-- =================================================================================
-- V4: Security Hardening - Locking, Timestamps
-- =================================================================================

-- 1. Add optimistic locking version column to aml_users
ALTER TABLE aml_users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Add timed lockout column (auto-unlock after duration)
ALTER TABLE aml_users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;

-- 3. Ensure created_at uses DB-level timestamp default
ALTER TABLE aml_users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE aml_system_audit_logs ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
