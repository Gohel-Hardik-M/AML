ALTER TABLE aml_global_rule_catalog
    ADD COLUMN IF NOT EXISTS description VARCHAR(255);

UPDATE aml_global_rule_catalog
SET description = CASE typology_name
    WHEN 'STRUCTURING_001' THEN 'Structuring / Smurfing Cash Deposits'
    WHEN 'VELOCITY_001' THEN 'Rapid Transaction Velocity Check'
    WHEN 'CROSS_BORDER_001' THEN 'Cross-Border High Value Transfer'
    WHEN 'CRYPTO_001' THEN 'Cryptocurrency Transaction Pattern'
    WHEN 'GEO_RISK_001' THEN 'High-Risk Geographic Route'
    WHEN 'SMURFING_001' THEN 'Smurfing Layering Network'
    ELSE typology_name
END
WHERE description IS NULL;