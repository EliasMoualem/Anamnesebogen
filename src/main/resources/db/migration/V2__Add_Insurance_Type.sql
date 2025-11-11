-- ===================================
-- Add Insurance Type to Patients Table
-- Version: 2
-- Description: Adds insurance_type column to distinguish between
--              self-insured (Selbstversichert) and family-insured (Familienversichert) patients
-- ===================================

-- Add insurance_type column to patients table
ALTER TABLE patients
ADD COLUMN insurance_type VARCHAR(20) NOT NULL DEFAULT 'SELF_INSURED';

-- Add check constraint for valid insurance types
ALTER TABLE patients
ADD CONSTRAINT chk_insurance_type CHECK (insurance_type IN ('SELF_INSURED', 'FAMILY_INSURED'));

-- Create index for insurance type queries
CREATE INDEX idx_patient_insurance_type ON patients(insurance_type);

-- Add comments for documentation
COMMENT ON COLUMN patients.insurance_type IS 'Type of insurance: SELF_INSURED (Selbstversichert) or FAMILY_INSURED (Familienversichert)';

-- Update guardians table comment to reflect its purpose as Hauptversicherter
COMMENT ON TABLE guardians IS 'Policyholders (Hauptversicherte) for FAMILY_INSURED patients. Represents the main insurance policyholder under whose policy the patient is covered.';
