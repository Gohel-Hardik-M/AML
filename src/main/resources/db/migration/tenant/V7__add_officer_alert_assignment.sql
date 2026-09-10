-- =================================================================================
-- V7: Add Compliance Officer Alert Assignment
-- =================================================================================

-- 1. Add assigned_officer_id to aml_alerts (the officer who is assigned to review this alert)
ALTER TABLE aml_alerts ADD COLUMN IF NOT EXISTS assigned_officer_id UUID;

-- 2. Index for fast lookup: "give me all alerts assigned to this officer"
CREATE INDEX IF NOT EXISTS idx_alerts_assigned_officer ON aml_alerts(assigned_officer_id);
