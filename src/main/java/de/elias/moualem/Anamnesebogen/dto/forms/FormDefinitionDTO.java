package de.elias.moualem.Anamnesebogen.dto.forms;

import com.fasterxml.jackson.databind.JsonNode;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for FormDefinition entity - used for API responses.
 * Contains complete form definition data including translations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinitionDTO {

    private UUID id;

    private String name;

    private String description;

    private FormCategory category;

    private String version;

    private FormStatus status;

    private Boolean isActive;

    private Boolean isDefault;

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
     * Example: {"vorname": "FIRST_NAME", "nachname": "LAST_NAME"}
     */
    private JsonNode fieldMappings;

    /**
     * Translations for this form in different languages.
     */
    @Builder.Default
    private List<FormTranslationDTO> translations = new ArrayList<>();

    /**
     * Number of submissions that used this form definition.
     */
    private Long submissionCount;

    // Audit fields
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private LocalDateTime publishedAt;

    private String publishedBy;
}
