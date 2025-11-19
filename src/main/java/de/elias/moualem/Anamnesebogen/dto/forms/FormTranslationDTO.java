package de.elias.moualem.Anamnesebogen.dto.forms;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for FormTranslation entity.
 * Used for both creating/updating translations and API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTranslationDTO {

    private UUID id;

    /**
     * Language code: de, en, ar, ru
     * Must be one of the supported languages.
     */
    @NotBlank(message = "Language code is required")
    @Pattern(regexp = "^(de|en|ar|ru)$", message = "Language must be one of: de, en, ar, ru")
    private String language;

    /**
     * JSONB object containing all translatable strings for the form.
     * Structure:
     * {
     *   "pages": { "personalInfo": "Personal Information", ... },
     *   "fields": { "firstName": "First Name", ... },
     *   "placeholders": { "enterFirstName": "Enter your first name", ... },
     *   "options": { "yes": "Yes", "no": "No", ... },
     *   "buttons": { "next": "Next", "previous": "Previous", ... },
     *   "validation": { "required": "This field is required", ... },
     *   "messages": { "success": "Form submitted successfully", ... }
     * }
     */
    @NotNull(message = "Translations are required")
    private JsonNode translations;

    /**
     * Display name for the language (e.g., "Deutsch", "English").
     */
    private String languageDisplayName;

    /**
     * Whether this is a right-to-left language (e.g., Arabic).
     */
    private Boolean isRtl;

    // Audit fields
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;
}
