-- =================================================================================
-- V4: Security Hardening - Locking, JWT Blacklist, Timestamps
-- =================================================================================

-- 1. Add optimistic locking version column to aml_users
ALTER TABLE aml_users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Add timed lockout column (auto-unlock after duration)
ALTER TABLE aml_users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;

-- 3. JWT Token Blacklist for logout support
CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(64) NOT NULL,
    expiry TIMESTAMP WITH TIME ZONE NOT NULL,
    blacklisted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_blacklisted_token_hash ON blacklisted_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_blacklisted_token_expiry ON blacklisted_tokens(expiry);

-- 4. Ensure created_at uses DB-level timestamp default
ALTER TABLE aml_users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE aml_system_audit_logs ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
