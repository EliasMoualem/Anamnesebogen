-- ===================================
-- Anamnesebogen Initial Database Schema
-- Version: 1
-- Description: Creates all tables for GDPR-compliant dental anamnesis system
-- ===================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===================================
-- Patients Table
-- ===================================
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Personal Information (PII - will be encrypted in Phase 2)
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(20),
    street VARCHAR(255),
    zip_code VARCHAR(20),
    city VARCHAR(100),
    mobile_number VARCHAR(30),
    phone_number VARCHAR(30),
    email_address VARCHAR(255),
    language VARCHAR(10),

    -- Insurance Information
    insurance_provider VARCHAR(255),
    insurance_policy_number VARCHAR(100),
    insurance_group_number VARCHAR(100),
    policyholder_name VARCHAR(200),
    relationship_to_policyholder VARCHAR(50),

    -- Medical History
    allergies TEXT,
    current_medications TEXT,
    medical_conditions TEXT,
    previous_surgeries TEXT,
    primary_care_doctor VARCHAR(255),

    -- GDPR Compliance Fields
    data_processing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    data_processing_consent_date TIMESTAMP,
    data_retention_until DATE,
    deletion_requested BOOLEAN DEFAULT FALSE,
    deletion_requested_date TIMESTAMP,
    submission_ip_address VARCHAR(45),
    submission_user_agent VARCHAR(500),

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT chk_email_format CHECK (email_address ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$' OR email_address IS NULL)
);

CREATE INDEX idx_patient_email ON patients(email_address);
CREATE INDEX idx_patient_deleted ON patients(deleted_at);
CREATE INDEX idx_patient_retention ON patients(data_retention_until);
CREATE INDEX idx_patient_created ON patients(created_at);

-- ===================================
-- Guardians Table
-- ===================================
CREATE TABLE guardians (
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

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_guardian_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX idx_guardian_patient ON guardians(patient_id);
CREATE INDEX idx_guardian_email ON guardians(email_address);

-- ===================================
-- Form Submissions Table
-- ===================================
CREATE TABLE form_submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL,

    -- Submission Metadata
    submission_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    form_language VARCHAR(10),
    form_version VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_type VARCHAR(50),

    -- PDF Generation
    pdf_file_path VARCHAR(500),
    pdf_generated_at TIMESTAMP,
    pdf_hash VARCHAR(64),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    notes TEXT,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_submission_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT chk_submission_status CHECK (status IN ('SUBMITTED', 'COMPLETED', 'FAILED', 'ARCHIVED'))
);

CREATE INDEX idx_submission_date ON form_submissions(submission_date);
CREATE INDEX idx_submission_patient ON form_submissions(patient_id);
CREATE INDEX idx_submission_status ON form_submissions(status);

-- ===================================
-- Signatures Table
-- ===================================
CREATE TABLE signatures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL,
    form_submission_id UUID,

    -- Signature Data
    signature_data BYTEA NOT NULL,
    signature_hash VARCHAR(64) NOT NULL,
    signature_mime_type VARCHAR(50) DEFAULT 'image/png',

    -- Signature Metadata
    signed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    signature_type VARCHAR(20) NOT NULL DEFAULT 'SIMPLE',
    document_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_info VARCHAR(255),
    geolocation VARCHAR(100),

    -- Legal Compliance
    signer_name VARCHAR(200) NOT NULL,
    intent_statement TEXT,
    identity_confirmed BOOLEAN DEFAULT FALSE,
    identity_verification_method VARCHAR(100),

    -- Tamper Detection
    integrity_verified BOOLEAN DEFAULT FALSE,
    last_integrity_check TIMESTAMP,
    tampered BOOLEAN DEFAULT FALSE,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_signature_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_signature_submission FOREIGN KEY (form_submission_id) REFERENCES form_submissions(id) ON DELETE SET NULL,
    CONSTRAINT chk_signature_type CHECK (signature_type IN ('SIMPLE', 'ADVANCED', 'QUALIFIED')),
    CONSTRAINT chk_document_type CHECK (document_type IN ('EWE', 'HONORARVEREINBARUNG', 'VERLANGENSLEISTUNGEN', 'MKV_FUELLUNG', 'ORTHODONTIC', 'ANAMNESIS', 'GENERAL_CONSENT', 'DATA_PROCESSING_CONSENT', 'OTHER'))
);

CREATE INDEX idx_signature_date ON signatures(signed_at);
CREATE INDEX idx_signature_patient ON signatures(patient_id);
CREATE INDEX idx_signature_document ON signatures(document_type);
CREATE INDEX idx_signature_submission ON signatures(form_submission_id);
CREATE INDEX idx_signature_tampered ON signatures(tampered);

-- ===================================
-- Consents Table
-- ===================================
CREATE TABLE consents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL,
    signature_id UUID,

    -- Consent Information
    consent_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GRANTED',
    consent_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawal_date TIMESTAMP,
    consent_version VARCHAR(20) NOT NULL,
    consent_text TEXT NOT NULL,
    language VARCHAR(10),

    -- Capture Method
    capture_method VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_info VARCHAR(255),

    -- Proof of Consent
    proof_type VARCHAR(30) NOT NULL,
    proof_data TEXT,

    -- Validity Period
    expiry_date TIMESTAMP,
    requires_renewal BOOLEAN DEFAULT FALSE,
    renewal_period_months INTEGER,

    -- Withdrawal Information
    withdrawal_reason TEXT,
    withdrawal_method VARCHAR(30),

    -- Legal and Compliance
    legal_basis VARCHAR(100),
    processing_purpose TEXT,
    data_categories TEXT,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_consent_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_consent_signature FOREIGN KEY (signature_id) REFERENCES signatures(id) ON DELETE SET NULL,
    CONSTRAINT chk_consent_type CHECK (consent_type IN ('DATA_PROCESSING', 'MARKETING', 'DATA_SHARING', 'MEDICAL_TREATMENT', 'BILLING', 'MEDIA', 'RESEARCH', 'TELEMEDICINE', 'EMERGENCY_CONTACT', 'OTHER')),
    CONSTRAINT chk_consent_status CHECK (status IN ('GRANTED', 'WITHDRAWN', 'EXPIRED', 'PENDING')),
    CONSTRAINT chk_capture_method CHECK (capture_method IN ('WEB_FORM', 'MOBILE_APP', 'EMAIL', 'IN_PERSON', 'PHONE', 'PAPER', 'OTHER')),
    CONSTRAINT chk_proof_type CHECK (proof_type IN ('CHECKBOX', 'ELECTRONIC_SIGNATURE', 'HANDWRITTEN_SIGNATURE', 'VERBAL', 'TWO_FACTOR', 'BIOMETRIC', 'OTHER'))
);

CREATE INDEX idx_consent_patient ON consents(patient_id);
CREATE INDEX idx_consent_type ON consents(consent_type);
CREATE INDEX idx_consent_status ON consents(status);
CREATE INDEX idx_consent_date ON consents(consent_date);
CREATE INDEX idx_consent_expiry ON consents(expiry_date);

-- ===================================
-- Audit Logs Table
-- ===================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- When & Who
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID,
    username VARCHAR(255),
    user_role VARCHAR(50),
    patient_id UUID,

    -- What Action
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    description TEXT,

    -- Where & How
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_info VARCHAR(255),
    request_method VARCHAR(10),
    request_uri VARCHAR(500),
    session_id VARCHAR(255),

    -- What Changed
    old_values TEXT,
    new_values TEXT,
    modified_fields VARCHAR(500),

    -- Result & Status
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    status_code INTEGER,
    duration_ms BIGINT,

    -- GDPR & Compliance
    legal_basis VARCHAR(100),
    access_purpose VARCHAR(255),
    data_categories VARCHAR(500),
    patient_notified BOOLEAN DEFAULT FALSE,

    -- Security & Alerts
    security_level VARCHAR(20) DEFAULT 'NORMAL',
    security_alert BOOLEAN DEFAULT FALSE,
    alert_reason VARCHAR(500),

    CONSTRAINT chk_action_type CHECK (action_type IN (
        'VIEW', 'EXPORT', 'DOWNLOAD', 'SEARCH', 'LIST',
        'CREATE', 'UPDATE', 'DELETE', 'SOFT_DELETE', 'RESTORE',
        'LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'PASSWORD_RESET', 'MFA_SETUP',
        'CONSENT_GRANTED', 'CONSENT_WITHDRAWN', 'CONSENT_UPDATED',
        'DATA_ACCESS_REQUEST', 'DATA_EXPORT_REQUEST', 'DATA_DELETION_REQUEST', 'DATA_RECTIFICATION_REQUEST', 'DATA_PORTABILITY_REQUEST',
        'SIGNATURE_CAPTURED', 'SIGNATURE_VERIFIED',
        'FORM_SUBMITTED', 'FORM_VIEWED', 'PDF_GENERATED',
        'BACKUP_CREATED', 'BACKUP_RESTORED', 'CONFIG_CHANGED', 'USER_CREATED', 'USER_DELETED', 'ROLE_CHANGED',
        'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY', 'DATA_BREACH_DETECTED',
        'OTHER'
    )),
    CONSTRAINT chk_security_level CHECK (security_level IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action_type);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_patient ON audit_logs(patient_id);
CREATE INDEX idx_audit_security ON audit_logs(security_level);
CREATE INDEX idx_audit_alert ON audit_logs(security_alert);

-- ===================================
-- Functions & Triggers
-- ===================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_patients_updated_at BEFORE UPDATE ON patients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_guardians_updated_at BEFORE UPDATE ON guardians
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_form_submissions_updated_at BEFORE UPDATE ON form_submissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_signatures_updated_at BEFORE UPDATE ON signatures
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_consents_updated_at BEFORE UPDATE ON consents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ===================================
-- Comments for Documentation
-- ===================================

COMMENT ON TABLE patients IS 'GDPR-compliant patient records with consent tracking and data retention management';
COMMENT ON TABLE guardians IS 'Legal guardians or parents for minor patients';
COMMENT ON TABLE form_submissions IS 'Audit trail of all form submissions and PDF generations';
COMMENT ON TABLE signatures IS 'Electronic signatures with tamper detection and eIDAS compliance tracking';
COMMENT ON TABLE consents IS 'GDPR Article 7 compliant consent records with full audit trail';
COMMENT ON TABLE audit_logs IS 'Comprehensive audit log for GDPR Article 30 compliance (Records of processing activities)';

COMMENT ON COLUMN patients.data_retention_until IS 'German law: 10 years retention for medical records';
COMMENT ON COLUMN patients.deletion_requested IS 'GDPR Article 17: Right to erasure (Right to be forgotten)';
COMMENT ON COLUMN signatures.signature_type IS 'eIDAS signature types: SIMPLE, ADVANCED, or QUALIFIED';
COMMENT ON COLUMN consents.consent_version IS 'Version of consent text for audit trail';

-- Grant permissions (will be executed after user creation)
-- These will work once the database user is created
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'anamnesebogen_user') THEN
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO anamnesebogen_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO anamnesebogen_user;
    END IF;
END
$$;
