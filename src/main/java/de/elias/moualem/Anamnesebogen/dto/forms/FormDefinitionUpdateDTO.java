package de.elias.moualem.Anamnesebogen.dto.forms;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing FormDefinition.
 * Only allows updating specific fields, not status or category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinitionUpdateDTO {

    @NotBlank(message = "Form name is required")
    private String name;

    private String description;

    @NotBlank(message = "Form version is required")
    private String version;

    /**
     * JSON Schema defining validation rules for form fields.
     */
    private JsonNode schema;

    /**
     * UI Schema defining form structure, fields, pages, and layout.
     */
    private JsonNode uiSchema;

    /**
     * Additional validation rules beyond JSON Schema.
     */
    private JsonNode validationRules;

    /**
     * UI rendering options (theme, layout preferences, etc.).
     */
    private JsonNode renderingOptions;

    /**
     * Field mappings from schema field names to canonical field types.
     * Example: {"vorname": "FIRST_NAME", "nachname": "LAST_NAME", "geburtsdatum": "BIRTH_DATE"}
     * Used to map dynamic form fields to Patient entity properties during submission processing.
     */
    private JsonNode fieldMappings;

    /**
     * Set this form as the default for its category.
     * When set to true, all other forms in the same category will have isDefault set to false.
     */
    private Boolean isDefault;
}
