-- ===================================
-- Rename Guardians Table to Policyholders
-- Version: 3
-- Description: Renames guardians table to policyholders to better reflect
--              German insurance terminology (Hauptversicherter = main policyholder)
-- ===================================

-- Rename the table
ALTER TABLE guardians RENAME TO policyholders;

-- Update table comment
COMMENT ON TABLE policyholders IS 'Policyholders (Hauptversicherte) for FAMILY_INSURED patients. Represents the main insurance policyholder (Hauptversicherter) under whose policy the patient (mitversicherte Person) is covered.';

-- Rename indexes (PostgreSQL renames them automatically with the table, but let's be explicit for clarity)
ALTER INDEX IF EXISTS idx_guardian_patient RENAME TO idx_policyholder_patient;
ALTER INDEX IF EXISTS idx_guardian_email RENAME TO idx_policyholder_email;

-- Note: Foreign key constraint names are automatically updated by PostgreSQL when renaming the table
-- The FK constraint fk_guardian_patient will be automatically renamed to reference the new table name

-- Update documentation comments
COMMENT ON COLUMN policyholders.patient_id IS 'The patient (dependent/mitversicherte Person) covered under this policyholder''s insurance';
COMMENT ON COLUMN policyholders.first_name IS 'First name of the policyholder (Hauptversicherter)';
COMMENT ON COLUMN policyholders.last_name IS 'Last name of the policyholder (Hauptversicherter)';
