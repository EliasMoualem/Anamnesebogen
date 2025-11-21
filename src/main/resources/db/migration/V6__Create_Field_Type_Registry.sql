-- V6__Create_Field_Type_Registry.sql
-- Creates field type registry system for mapping dynamic form fields to Patient entity properties

-- ============================================================================
-- 1. Create field_types table
-- ============================================================================
CREATE TABLE field_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_type VARCHAR(100) UNIQUE NOT NULL,  -- "FIRST_NAME", "LAST_NAME", etc.
    canonical_name VARCHAR(100) NOT NULL,      -- "firstName", "lastName" (Patient entity property)
    display_name_key VARCHAR(100),             -- "field.firstName" (i18n key for translations)
    category VARCHAR(50) NOT NULL,             -- "PERSONAL", "CONTACT", "INSURANCE", "MEDICAL", "CONSENT", "CUSTOM"
    data_type VARCHAR(50) NOT NULL,            -- "STRING", "TEXT", "DATE", "EMAIL", "PHONE", "NUMBER", "BOOLEAN", "SIGNATURE"
    is_required BOOLEAN DEFAULT false,         -- Must be mapped for form to publish
    is_system BOOLEAN DEFAULT true,            -- System fields vs. custom fields added by users
    accepted_aliases JSONB,                    -- ["first_name", "vorname", "prénom"] (for future import/migration)
    validation_rules JSONB,                    -- Default JSON Schema validation rules
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create index for fast lookups
CREATE INDEX idx_field_types_field_type ON field_types(field_type);
CREATE INDEX idx_field_types_category ON field_types(category);
CREATE INDEX idx_field_types_is_required ON field_types(is_required);

-- ============================================================================
-- 2. Seed system field types
-- ============================================================================

-- Personal Information Fields
INSERT INTO field_types (field_type, canonical_name, display_name_key, category, data_type, is_required, accepted_aliases, validation_rules) VALUES
    ('FIRST_NAME', 'firstName', 'field.firstName', 'PERSONAL', 'STRING', true,
     '["first_name", "vorname", "prénom", "nombre", "nome"]'::jsonb,
     '{"type": "string", "minLength": 1, "maxLength": 100}'::jsonb),

    ('LAST_NAME', 'lastName', 'field.lastName', 'PERSONAL', 'STRING', true,
     '["last_name", "nachname", "nom", "apellido", "cognome", "familienname", "surname"]'::jsonb,
     '{"type": "string", "minLength": 1, "maxLength": 100}'::jsonb),

    ('BIRTH_DATE', 'birthDate', 'field.birthDate', 'PERSONAL', 'DATE', true,
     '["birth_date", "geburtsdatum", "date_of_birth", "dob", "dateOfBirth", "date_naissance"]'::jsonb,
     '{"type": "string", "format": "date"}'::jsonb),

    ('GENDER', 'gender', 'field.gender', 'PERSONAL', 'STRING', false,
     '["geschlecht", "sexe", "sex", "genero"]'::jsonb,
     '{"type": "string", "enum": ["MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"]}'::jsonb);

-- Contact Information Fields
INSERT INTO field_types (field_type, canonical_name, display_name_key, category, data_type, is_required, accepted_aliases, validation_rules) VALUES
    ('EMAIL', 'email', 'field.email', 'CONTACT', 'EMAIL', false,
     '["email_address", "e_mail", "mail", "emailAddress"]'::jsonb,
     '{"type": "string", "format": "email", "maxLength": 255}'::jsonb),

    ('PHONE', 'phone', 'field.phone', 'CONTACT', 'PHONE', false,
     '["phone_number", "telefon", "telephone", "phoneNumber", "tel"]'::jsonb,
     '{"type": "string", "pattern": "^[+]?[0-9\\s\\-()]+$", "maxLength": 20}'::jsonb),

    ('MOBILE', 'mobile', 'field.mobile', 'CONTACT', 'PHONE', false,
     '["mobile_number", "mobilnummer", "handy", "cell", "mobileNumber", "cellular"]'::jsonb,
     '{"type": "string", "pattern": "^[+]?[0-9\\s\\-()]+$", "maxLength": 20}'::jsonb),

    ('STREET', 'street', 'field.street', 'CONTACT', 'STRING', false,
     '["strasse", "address", "rue", "calle", "via", "streetAddress"]'::jsonb,
     '{"type": "string", "maxLength": 255}'::jsonb),

    ('ZIP_CODE', 'zipCode', 'field.zipCode', 'CONTACT', 'STRING', false,
     '["plz", "postal_code", "postalCode", "zip", "postcode", "code_postal"]'::jsonb,
     '{"type": "string", "maxLength": 10}'::jsonb),

    ('CITY', 'city', 'field.city', 'CONTACT', 'STRING', false,
     '["stadt", "ort", "ville", "ciudad", "citta"]'::jsonb,
     '{"type": "string", "maxLength": 100}'::jsonb),

    ('COUNTRY', 'country', 'field.country', 'CONTACT', 'STRING', false,
     '["land", "pays", "pais", "paese"]'::jsonb,
     '{"type": "string", "maxLength": 100}'::jsonb);

-- Insurance Fields
INSERT INTO field_types (field_type, canonical_name, display_name_key, category, data_type, is_required, accepted_aliases, validation_rules) VALUES
    ('INSURANCE_TYPE', 'insuranceType', 'field.insuranceType', 'INSURANCE', 'STRING', true,
     '["versicherungsart", "insurance", "assurance_type"]'::jsonb,
     '{"type": "string", "enum": ["SELF_INSURED", "FAMILY_INSURED"]}'::jsonb),

    ('INSURANCE_NUMBER', 'insuranceNumber', 'field.insuranceNumber', 'INSURANCE', 'STRING', false,
     '["versicherungsnummer", "insurance_id", "policy_number"]'::jsonb,
     '{"type": "string", "maxLength": 50}'::jsonb),

    ('INSURANCE_COMPANY', 'insuranceCompany', 'field.insuranceCompany', 'INSURANCE', 'STRING', false,
     '["versicherung", "insurance_provider", "krankenkasse"]'::jsonb,
     '{"type": "string", "maxLength": 255}'::jsonb);

-- Medical History Fields
INSERT INTO field_types (field_type, canonical_name, display_name_key, category, data_type, is_required, accepted_aliases, validation_rules) VALUES
    ('MEDICAL_HISTORY', 'medicalHistory', 'field.medicalHistory', 'MEDICAL', 'TEXT', false,
     '["krankengeschichte", "medical_conditions", "health_history", "anamnese"]'::jsonb,
     '{"type": "string", "maxLength": 5000}'::jsonb),

    ('ALLERGIES', 'allergies', 'field.allergies', 'MEDICAL', 'TEXT', false,
     '["allergien", "allergie", "allergy"]'::jsonb,
     '{"type": "string", "maxLength": 2000}'::jsonb),

    ('MEDICATIONS', 'medications', 'field.medications', 'MEDICAL', 'TEXT', false,
     '["medikamente", "medicine", "drugs", "current_medications"]'::jsonb,
     '{"type": "string", "maxLength": 2000}'::jsonb),

    ('CURRENT_COMPLAINTS', 'currentComplaints', 'field.currentComplaints', 'MEDICAL', 'TEXT', false,
     '["beschwerden", "complaints", "symptoms", "chief_complaint"]'::jsonb,
     '{"type": "string", "maxLength": 2000}'::jsonb);

-- Signature & Consent Fields
INSERT INTO field_types (field_type, canonical_name, display_name_key, category, data_type, is_required, accepted_aliases, validation_rules) VALUES
    ('PATIENT_SIGNATURE', 'patientSignature', 'field.patientSignature', 'CONSENT', 'SIGNATURE', false,
     '["signature", "unterschrift", "sig", "patient_sig"]'::jsonb,
     '{"type": "string", "format": "signature"}'::jsonb),

    ('CONSENT_DATA_PROCESSING', 'dataProcessingConsent', 'field.dataProcessingConsent', 'CONSENT', 'BOOLEAN', true,
     '["data_consent", "datenschutz", "privacy_consent", "gdpr_consent"]'::jsonb,
     '{"type": "boolean"}'::jsonb),

    ('CONSENT_TREATMENT', 'treatmentConsent', 'field.treatmentConsent', 'CONSENT', 'BOOLEAN', false,
     '["treatment_consent", "behandlungseinwilligung", "consent"]'::jsonb,
     '{"type": "boolean"}'::jsonb);

-- Guardian/Policyholder Fields (for minor patients)
INSERT INTO field_types (field_type, canonical_name, display_name_key, category, data_type, is_required, accepted_aliases, validation_rules) VALUES
    ('GUARDIAN_FIRST_NAME', 'guardianFirstName', 'field.guardianFirstName', 'PERSONAL', 'STRING', false,
     '["guardian_first_name", "erziehungsberechtigter_vorname", "legal_guardian_first"]'::jsonb,
     '{"type": "string", "maxLength": 100}'::jsonb),

    ('GUARDIAN_LAST_NAME', 'guardianLastName', 'field.guardianLastName', 'PERSONAL', 'STRING', false,
     '["guardian_last_name", "erziehungsberechtigter_nachname", "legal_guardian_last"]'::jsonb,
     '{"type": "string", "maxLength": 100}'::jsonb),

    ('GUARDIAN_RELATIONSHIP', 'guardianRelationship', 'field.guardianRelationship', 'PERSONAL', 'STRING', false,
     '["relationship", "beziehung", "relation"]'::jsonb,
     '{"type": "string", "enum": ["MOTHER", "FATHER", "LEGAL_GUARDIAN", "OTHER"]}'::jsonb);

-- ============================================================================
-- 3. Add field_mappings column to form_definitions table
-- ============================================================================
ALTER TABLE form_definitions
    ADD COLUMN field_mappings JSONB DEFAULT '{}'::jsonb;

COMMENT ON COLUMN form_definitions.field_mappings IS 'Maps schema field names to field_types. Format: {"schemaFieldName": "FIRST_NAME", "anotherField": "EMAIL"}';

-- Create GIN index for JSONB field_mappings for fast queries
CREATE INDEX idx_form_definitions_field_mappings ON form_definitions USING gin(field_mappings);

-- ============================================================================
-- 4. Comments for documentation
-- ============================================================================
COMMENT ON TABLE field_types IS 'Registry of canonical field types that map dynamic form fields to Patient entity properties';
COMMENT ON COLUMN field_types.field_type IS 'Unique identifier for field type (e.g., FIRST_NAME, EMAIL)';
COMMENT ON COLUMN field_types.canonical_name IS 'Patient entity property name (e.g., firstName, email)';
COMMENT ON COLUMN field_types.display_name_key IS 'i18n translation key for UI display';
COMMENT ON COLUMN field_types.category IS 'Field category: PERSONAL, CONTACT, INSURANCE, MEDICAL, CONSENT, CUSTOM';
COMMENT ON COLUMN field_types.data_type IS 'Data type: STRING, TEXT, DATE, EMAIL, PHONE, NUMBER, BOOLEAN, SIGNATURE';
COMMENT ON COLUMN field_types.is_required IS 'Whether this field must be mapped for form to be published';
COMMENT ON COLUMN field_types.is_system IS 'System-defined field (true) vs. user-created custom field (false)';
COMMENT ON COLUMN field_types.accepted_aliases IS 'JSONB array of alternative field names for auto-detection and migration';
COMMENT ON COLUMN field_types.validation_rules IS 'Default JSON Schema validation rules applied to this field type';
