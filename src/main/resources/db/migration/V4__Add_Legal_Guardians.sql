-- ===================================
-- Add Legal Guardians Table
-- Version: 4
-- Description: Creates legal_guardians table for minor patients (under 18 years old).
--              Separates legal guardianship (Erziehungsberechtigter) from insurance
--              policyholders (Hauptversicherter), as they may be different people.
--
-- Business Rules:
--   - All minor patients (< 18 years) MUST have a legal guardian
--   - All minor patients MUST be FAMILY_INSURED (no self-insured minors)
--   - Guardian and policyholder may be the same person, but are tracked separately
-- ===================================

-- ===================================
-- Legal Guardians Table
-- ===================================
CREATE TABLE legal_guardians (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL UNIQUE,

    -- Personal Information
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    gender VARCHAR(20),
    street VARCHAR(255),
    zip_code VARCHAR(20),
    city VARCHAR(100),
    mobile_number VARCHAR(30),
    phone_number VARCHAR(30),
    email_address VARCHAR(255),
    job VARCHAR(255),

    -- Guardian-Specific Fields
    relationship_type VARCHAR(30) NOT NULL,
    -- Possible values: MOTHER, FATHER, LEGAL_GUARDIAN, GRANDPARENT, OTHER

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_legal_guardian_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT chk_relationship_type CHECK (relationship_type IN ('MOTHER', 'FATHER', 'LEGAL_GUARDIAN', 'GRANDPARENT', 'OTHER'))
);

-- Create indexes for performance
CREATE INDEX idx_guardian_patient ON legal_guardians(patient_id);
CREATE INDEX idx_guardian_email ON legal_guardians(email_address);
CREATE INDEX idx_guardian_relationship ON legal_guardians(relationship_type);

-- Add comments for documentation
COMMENT ON TABLE legal_guardians IS 'Legal guardians (Erziehungsberechtigte) for minor patients (under 18 years old). Represents the person with legal custody who is authorized to make medical decisions. Separate from insurance policyholders.';
COMMENT ON COLUMN legal_guardians.patient_id IS 'The minor patient for whom this person is the legal guardian';
COMMENT ON COLUMN legal_guardians.relationship_type IS 'Relationship of guardian to patient: MOTHER, FATHER, LEGAL_GUARDIAN, GRANDPARENT, OTHER';
COMMENT ON COLUMN legal_guardians.first_name IS 'First name of the legal guardian (Erziehungsberechtigter)';
COMMENT ON COLUMN legal_guardians.last_name IS 'Last name of the legal guardian (Erziehungsberechtigter)';
