-- ============================================================================
-- Migration V5: Add Form Builder Tables
-- Description: Add tables for dynamic form builder functionality
-- Date: 2025-01-11
-- ============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- form_definitions table
-- Stores form structure, validation rules, and UI schema
-- ============================================================================
CREATE TABLE form_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,

    -- Form structure stored as JSONB
    schema JSONB NOT NULL,
    ui_schema JSONB NOT NULL,
    validation_rules JSONB,
    rendering_options JSONB,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    published_at TIMESTAMP,
    published_by VARCHAR(255),

    -- Constraints
    CONSTRAINT chk_form_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_form_category CHECK (category IN ('ANAMNESIS', 'CONSENT', 'TREATMENT', 'CUSTOM'))
);

-- Indexes for form_definitions
CREATE INDEX idx_form_definitions_status ON form_definitions(status);
CREATE INDEX idx_form_definitions_category ON form_definitions(category);
CREATE INDEX idx_form_definitions_is_active ON form_definitions(is_active);
CREATE INDEX idx_form_definitions_created_at ON form_definitions(created_at DESC);

-- GIN indexes for JSONB columns (enables fast JSON queries)
CREATE INDEX idx_form_definitions_schema ON form_definitions USING GIN (schema);
CREATE INDEX idx_form_definitions_ui_schema ON form_definitions USING GIN (ui_schema);

-- Comments for documentation
COMMENT ON TABLE form_definitions IS 'Stores form structure and configuration for dynamic form builder';
COMMENT ON COLUMN form_definitions.schema IS 'JSON Schema validation rules for form fields';
COMMENT ON COLUMN form_definitions.ui_schema IS 'UI structure including fields, pages, layout, and field properties';
COMMENT ON COLUMN form_definitions.validation_rules IS 'Additional validation rules beyond JSON Schema';
COMMENT ON COLUMN form_definitions.rendering_options IS 'UI rendering options (theme, layout preferences)';

-- ============================================================================
-- form_translations table
-- Stores multi-language translations for forms
-- ============================================================================
CREATE TABLE form_translations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    form_definition_id UUID NOT NULL REFERENCES form_definitions(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    translations JSONB NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    -- Constraints
    UNIQUE(form_definition_id, language),
    CONSTRAINT chk_language CHECK (language IN ('de', 'en', 'ar', 'ru'))
);

-- Indexes for form_translations
CREATE INDEX idx_form_translations_form_id ON form_translations(form_definition_id);
CREATE INDEX idx_form_translations_language ON form_translations(language);
CREATE INDEX idx_form_translations_translations ON form_translations USING GIN (translations);

-- Comments
COMMENT ON TABLE form_translations IS 'Stores multi-language translations for form labels, placeholders, and validation messages';
COMMENT ON COLUMN form_translations.translations IS 'JSONB object containing all translatable strings for the form';

-- ============================================================================
-- Modify patients table to add custom_fields JSONB column
-- ============================================================================
ALTER TABLE patients ADD COLUMN IF NOT EXISTS custom_fields JSONB;

-- GIN index for fast queries on custom fields
CREATE INDEX IF NOT EXISTS idx_patients_custom_fields ON patients USING GIN (custom_fields);

-- Comment
COMMENT ON COLUMN patients.custom_fields IS 'Dynamic fields from form builder stored as JSONB. Allows flexible form fields without schema changes.';

-- ============================================================================
-- Modify form_submissions table to track which form was used
-- ============================================================================
ALTER TABLE form_submissions
    ADD COLUMN IF NOT EXISTS form_definition_id UUID REFERENCES form_definitions(id),
    ADD COLUMN IF NOT EXISTS form_version VARCHAR(20),
    ADD COLUMN IF NOT EXISTS form_data_snapshot JSONB;

-- Index for form_submissions
CREATE INDEX IF NOT EXISTS idx_form_submissions_form_definition_id ON form_submissions(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_form_submissions_form_data_snapshot ON form_submissions USING GIN (form_data_snapshot);

-- Comments
COMMENT ON COLUMN form_submissions.form_definition_id IS 'Reference to the form definition used for this submission';
COMMENT ON COLUMN form_submissions.form_version IS 'Version of the form at time of submission';
COMMENT ON COLUMN form_submissions.form_data_snapshot IS 'Complete snapshot of form data at submission time (for historical record)';

-- ============================================================================
-- Insert default anamnesis form (placeholder - will be replaced with real data)
-- ============================================================================
INSERT INTO form_definitions (
    name,
    description,
    category,
    version,
    status,
    is_active,
    is_default,
    schema,
    ui_schema
) VALUES (
    'Default Anamnesis Form',
    'Initial placeholder form - will be replaced with migrated templates',
    'ANAMNESIS',
    '1.0.0',
    'DRAFT',
    false,
    true,
    '{"type": "object", "properties": {}}'::jsonb,
    '{"pages": []}'::jsonb
);

-- ============================================================================
-- Create view for active forms (useful for queries)
-- ============================================================================
CREATE OR REPLACE VIEW active_forms AS
SELECT
    fd.id,
    fd.name,
    fd.category,
    fd.version,
    fd.schema,
    fd.ui_schema,
    fd.published_at,
    json_agg(
        json_build_object(
            'language', ft.language,
            'translations', ft.translations
        )
    ) as all_translations
FROM form_definitions fd
LEFT JOIN form_translations ft ON fd.id = ft.form_definition_id
WHERE fd.is_active = true AND fd.status = 'PUBLISHED'
GROUP BY fd.id, fd.name, fd.category, fd.version, fd.schema, fd.ui_schema, fd.published_at;

COMMENT ON VIEW active_forms IS 'View of all active published forms with their translations';

-- ============================================================================
-- End of migration
-- ============================================================================