-- =================================================================================
-- V4: Tenant Rule Allocation Table
-- =================================================================================

-- This table tracks which rules the System Admin has allocated to which tenant.
-- The System Admin picks from the global rule catalog and assigns to specific tenants.
CREATE TABLE IF NOT EXISTS aml_tenant_rule_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    allocated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_rule_alloc UNIQUE (tenant_id, rule_code)
);

-- Seed the global rule catalog with our 6 existing rules
INSERT INTO aml_global_rule_catalog (typology_name, default_thresholds) VALUES
    ('STRUCTURING_001', '{"threshold_amount": 10000}'),
    ('VELOCITY_001', '{"window_minutes": 60, "max_count": 5}'),
    ('CROSS_BORDER_001', '{"threshold_amount": 50000}'),
    ('CRYPTO_001', '{"threshold_amount": 5000}'),
    ('GEO_RISK_001', '{"threshold_amount": 25000}'),
    ('SMURFING_001', '{"threshold_amount": 3000, "max_count": 3}')
ON CONFLICT (typology_name) DO NOTHING;
