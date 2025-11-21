package de.elias.moualem.Anamnesebogen.dto.forms;

import com.fasterxml.jackson.databind.JsonNode;
import de.elias.moualem.Anamnesebogen.entity.FieldType.FieldCategory;
import de.elias.moualem.Anamnesebogen.entity.FieldType.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for FieldType entity - used for API responses.
 * Contains field type metadata for displaying in form builder dropdowns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldTypeDTO {

    /**
     * Unique identifier for the field type record.
     */
    private UUID id;

    /**
     * Unique identifier for the field type (e.g., "FIRST_NAME", "EMAIL").
     * Used in form_definitions.field_mappings to reference this field type.
     */
    private String fieldType;

    /**
     * Canonical name matching the Patient entity property (e.g., "firstName", "email").
     * Used to set the correct property when processing form submissions.
     */
    private String canonicalName;

    /**
     * i18n translation key for UI display (e.g., "field.firstName").
     * Used by frontend to show localized field labels.
     */
    private String displayNameKey;

    /**
     * Field category for grouping in UI dropdowns.
     * Example: PERSONAL, CONTACT, INSURANCE, MEDICAL, CONSENT, CUSTOM
     */
    private FieldCategory category;

    /**
     * Data type for validation and rendering hints.
     * Example: STRING, TEXT, DATE, EMAIL, PHONE, NUMBER, BOOLEAN, SIGNATURE
     */
    private DataType dataType;

    /**
     * Whether this field type must be mapped for a form to be published.
     * Required fields block form publishing if not mapped.
     */
    private Boolean isRequired;

    /**
     * Whether this is a system-defined field type (true) or user-created custom field (false).
     * System fields are seeded via migration and cannot be deleted.
     */
    private Boolean isSystem;

    /**
     * JSONB array of accepted field name aliases for auto-detection and migration.
     * Example: ["first_name", "vorname", "prénom", "nombre"]
     */
    private JsonNode acceptedAliases;

    /**
     * Default JSON Schema validation rules for this field type.
     * Example: {"type": "string", "format": "email", "maxLength": 255}
     */
    private JsonNode validationRules;

    /**
     * Timestamp when this field type was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this field type was last updated.
     */
    private LocalDateTime updatedAt;
}
