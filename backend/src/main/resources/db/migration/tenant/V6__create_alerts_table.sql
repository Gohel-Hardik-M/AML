CREATE TABLE IF NOT EXISTS aml_alerts (
                                          alert_id UUID PRIMARY KEY,
                                          transaction_id UUID NOT NULL REFERENCES transactions(transaction_id),
    customer_id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    triggered_amount NUMERIC(18,2) NOT NULL,
    narrative TEXT,
    detection_metadata_json TEXT,
    is_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_case_id UUID,
    batch_id UUID REFERENCES batches(id),
    tenant_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_aml_alerts_batch_id ON aml_alerts(batch_id);
CREATE INDEX IF NOT EXISTS idx_aml_alerts_transaction_id ON aml_alerts(transaction_id);
CREATE INDEX IF NOT EXISTS idx_aml_alerts_customer_id ON aml_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_aml_alerts_reviewed ON aml_alerts(is_reviewed);