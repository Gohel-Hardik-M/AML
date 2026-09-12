INSERT INTO aml_global_rule_catalog (typology_name, description, default_thresholds)
VALUES
    ('STRUCTURING_001', 'Structuring / Smurfing Cash Deposits', '{"threshold_amount": 10000}'::jsonb),
    ('VELOCITY_001', 'Rapid Transaction Velocity Check', '{"window_minutes": 60, "max_count": 5}'::jsonb),
    ('CROSS_BORDER_001', 'Cross-Border High Value Transfer', '{"threshold_amount": 50000}'::jsonb),
    ('GEO_RISK_001', 'High-Risk Geographic Route', '{"threshold_amount": 25000}'::jsonb),
    ('SMURFING_001', 'Smurfing Layering Network', '{"threshold_amount": 3000, "max_count": 3, "window_minutes": 1440}'::jsonb)
ON CONFLICT (typology_name) DO NOTHING;
