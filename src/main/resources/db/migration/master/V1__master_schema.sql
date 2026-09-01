-- =========================================================================================
-- MASTER DATABASE SCHEMA (Runs on aml_master)
-- =========================================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS aml_tenant_registry (
                                                   tenant_id VARCHAR(64) PRIMARY KEY, -- e.g., 'HDFC'
    bank_name VARCHAR(255) NOT NULL,
    db_url VARCHAR(255) NOT NULL,      -- e.g., 'jdbc:postgresql://localhost:5432/aml_hdfc'
    db_username VARCHAR(128) NOT NULL,
    db_password VARCHAR(128) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS aml_system_admins (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS aml_global_rule_catalog (
                                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    typology_name VARCHAR(255) NOT NULL UNIQUE,
    default_thresholds JSONB NOT NULL
    );