package de.elias.moualem.Anamnesebogen.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a canonical field type in the field type registry.
 * Maps dynamic form fields to Patient entity properties with validation rules.
 * Supports field name aliases for multilingual forms and auto-detection.
 */
@Entity
@Table(name = "field_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique identifier for the field type (e.g., "FIRST_NAME", "EMAIL").
     * Used in form_definitions.field_mappings to reference this field type.
     */
    @Column(name = "field_type", unique = true, nullable = false, length = 100)
    private String fieldType;

    /**
     * Canonical name matching the Patient entity property (e.g., "firstName", "email").
     * Used to set the correct property when processing form submissions.
     */
    @Column(name = "canonical_name", nullable = false, length = 100)
    private String canonicalName;

    /**
     * i18n translation key for UI display (e.g., "field.firstName").
     * Used by frontend to show localized field labels.
     */
    @Column(name = "display_name_key", length = 100)
    private String displayNameKey;

    /**
     * Field category for grouping in UI dropdowns.
     * Example: PERSONAL, CONTACT, INSURANCE, MEDICAL, CONSENT, CUSTOM
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FieldCategory category;

    /**
     * Data type for validation and rendering hints.
     * Example: STRING, TEXT, DATE, EMAIL, PHONE, NUMBER, BOOLEAN, SIGNATURE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private DataType dataType;

    /**
     * Whether this field type must be mapped for a form to be published.
     * Required fields block form publishing if not mapped.
     */
    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    /**
     * Whether this is a system-defined field type (true) or user-created custom field (false).
     * System fields are seeded via migration and cannot be deleted.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = true;

    /**
     * JSONB array of accepted field name aliases for auto-detection and migration.
     * Example: ["first_name", "vorname", "prénom", "nombre"]
     * Used to suggest mappings when importing forms or auto-detecting field types.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accepted_aliases", columnDefinition = "jsonb")
    private JsonNode acceptedAliases;

    /**
     * Default JSON Schema validation rules for this field type.
     * Example: {"type": "string", "format": "email", "maxLength": 255}
     * Applied automatically when a field is mapped to this type.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private JsonNode validationRules;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Enum defining field categories for UI grouping.
     */
    public enum FieldCategory {
        PERSONAL,    // Personal information (name, birth date, gender)
        CONTACT,     // Contact information (email, phone, address)
        INSURANCE,   // Insurance-related fields
        MEDICAL,     // Medical history, allergies, medications
        CONSENT,     // Signatures and consent checkboxes
        CUSTOM       // User-defined custom fields
    }

    /**
     * Enum defining data types for validation and rendering.
     */
    public enum DataType {
        STRING,      // Short text input (< 255 chars)
        TEXT,        // Long text input / textarea
        DATE,        // Date picker
        EMAIL,       // Email input with validation
        PHONE,       // Phone number input
        NUMBER,      // Numeric input
        BOOLEAN,     // Checkbox / toggle
        SIGNATURE    // Signature pad (base64 encoded)
    }
}
