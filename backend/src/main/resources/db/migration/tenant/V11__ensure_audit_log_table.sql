CREATE TABLE IF NOT EXISTS aml_system_audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128),
    action_type VARCHAR(64) NOT NULL,
    affected_record_id VARCHAR(128),
    ip_address VARCHAR(64),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_created_at ON aml_system_audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_type ON aml_system_audit_logs (action_type);