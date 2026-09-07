-- =================================================================================
-- V2: Security Hardening - Master Admin Lockout & JWT Blacklist
-- =================================================================================

-- 1. Add lockout tracking to system_admins
ALTER TABLE system_admins ADD COLUMN IF NOT EXISTS failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE system_admins ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE system_admins ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;

-- 2. JWT Token Blacklist (stored in master DB since JWTs are global)
CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(64) NOT NULL,
    expiry TIMESTAMP WITH TIME ZONE NOT NULL,
    blacklisted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_blacklisted_token_hash ON blacklisted_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_blacklisted_token_expiry ON blacklisted_tokens(expiry);
