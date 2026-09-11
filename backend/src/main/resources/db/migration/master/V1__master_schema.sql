CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Central Tenant Registry
CREATE TABLE IF NOT EXISTS aml_tenant_registry (
                                                   tenant_id VARCHAR(64) PRIMARY KEY,
                                                   bank_name VARCHAR(255) NOT NULL,
                                                   db_url VARCHAR(255) NOT NULL,
                                                   db_username VARCHAR(128) NOT NULL,
                                                   db_password VARCHAR(128) NOT NULL,
                                                   is_active BOOLEAN DEFAULT TRUE,
                                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_admins (
                                             admin_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             username VARCHAR(64) UNIQUE NOT NULL,
                                             email VARCHAR(255) NOT NULL UNIQUE,
                                             password_hash VARCHAR(255) NOT NULL,
                                             full_name VARCHAR(128),
                                             is_active BOOLEAN DEFAULT TRUE,
                                             last_login TIMESTAMP,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- 3. Global Rule Catalog
CREATE TABLE IF NOT EXISTS aml_global_rule_catalog (
                                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                       typology_name VARCHAR(255) NOT NULL UNIQUE,
                                                       default_thresholds JSONB NOT NULL
);