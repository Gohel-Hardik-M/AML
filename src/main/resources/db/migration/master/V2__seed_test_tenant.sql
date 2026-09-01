INSERT INTO aml_tenant_registry (tenant_id, bank_name, db_url, db_username, db_password)
VALUES (
           'HDFC',
           'HDFC Bank',
           'jdbc:postgresql://localhost:5432/aml_hdfc',
           'postgres',
           'admin123'
       )
ON CONFLICT (tenant_id) DO NOTHING;