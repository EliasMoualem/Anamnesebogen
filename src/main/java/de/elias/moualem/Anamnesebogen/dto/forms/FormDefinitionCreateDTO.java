package de.elias.moualem.Anamnesebogen.dto.forms;

import com.fasterxml.jackson.databind.JsonNode;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new FormDefinition.
 * Contains only the fields needed to create a form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinitionCreateDTO {

    @NotBlank(message = "Form name is required")
    private String name;

    private String description;

    @NotNull(message = "Form category is required")
    private FormCategory category;

    @NotBlank(message = "Form version is required")
    private String version;

    /**
     * JSON Schema defining validation rules for form fields.
     * Required field - form must have a schema.
     */
    @NotNull(message = "Form schema is required")
    private JsonNode schema;

    /**
     * UI Schema defining form structure, fields, pages, and layout.
     * Required field - form must have a UI schema.
     */
    @NotNull(message = "Form UI schema is required")
    private JsonNode uiSchema;

    /**
     * Additional validation rules beyond JSON Schema.
     * Optional field.
     */
    private JsonNode validationRules;

    /**
     * UI rendering options (theme, layout preferences, etc.).
     * Optional field.
     */
    private JsonNode renderingOptions;

    /**
     * Field mappings from schema field names to canonical field types.
     * Example: {"vorname": "FIRST_NAME", "nachname": "LAST_NAME", "geburtsdatum": "BIRTH_DATE"}
     * Used to map dynamic form fields to Patient entity properties during submission processing.
     * Optional field - can be set during form creation or added later before publishing.
     */
    private JsonNode fieldMappings;

    /**
     * Set this form as the default for its category.
     * Optional, defaults to false.
     */
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * User who is creating the form.
     * Optional, can be set from security context.
     */
    private String createdBy;
}
