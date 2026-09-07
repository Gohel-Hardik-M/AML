-- =================================================================================
-- V2: Security Hardening - Master Admin Lockout
-- =================================================================================

-- 1. Add lockout tracking to system_admins
ALTER TABLE system_admins ADD COLUMN IF NOT EXISTS failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE system_admins ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE system_admins ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;
