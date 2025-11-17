package de.elias.moualem.Anamnesebogen.controller.forms;

import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.service.forms.FormDefinitionService;
import de.elias.moualem.Anamnesebogen.service.forms.FormTranslationService;
import de.elias.moualem.Anamnesebogen.service.forms.ThymeleafTemplateGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for Thymeleaf template generation and preview.
 * Provides endpoints for generating HTML templates from form definitions.
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@Slf4j
public class FormTemplateController {

    private final FormDefinitionService formDefinitionService;
    private final FormTranslationService formTranslationService;
    private final ThymeleafTemplateGenerator templateGenerator;
    private final CacheManager cacheManager;

    /**
     * Generate Thymeleaf template HTML for a form definition.
     * GET /api/forms/{id}/template?lang=de
     *
     * @param id       Form definition ID
     * @param language Language code (de, en, ar, ru)
     * @return Generated Thymeleaf HTML template
     */
    @GetMapping(value = "/{id}/template", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generateTemplate(
            @PathVariable UUID id,
            @RequestParam(value = "lang", defaultValue = "de") String language
    ) {
        log.info("Generating Thymeleaf template for form={}, language={}", id, language);

        try {
            FormDefinition formDefinition = formDefinitionService.getFormById(id);

            // Load translation if available
            FormTranslationDTO translation = null;
            try {
                translation = formTranslationService.getTranslation(id, language);
            } catch (Exception e) {
                log.debug("No translation found for form {} in language {}", id, language);
            }

            // Generate template (will be cached)
            String template = templateGenerator.generateTemplate(formDefinition, language, translation);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(template);

        } catch (Exception e) {
            log.error("Failed to generate template for form {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("<html><body><h1>Error generating template</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }

    /**
     * Generate preview HTML for admin UI.
     * GET /api/forms/{id}/preview-html?lang=de
     *
     * @param id       Form definition ID
     * @param language Language code
     * @return Rendered HTML preview
     */
    @GetMapping(value = "/{id}/preview-html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generatePreviewHtml(
            @PathVariable UUID id,
            @RequestParam(value = "lang", defaultValue = "de") String language
    ) {
        log.info("Generating preview HTML for form={}, language={}", id, language);

        try {
            FormDefinition formDefinition = formDefinitionService.getFormById(id);

            // Load translation if available
            FormTranslationDTO translation = null;
            try {
                translation = formTranslationService.getTranslation(id, language);
            } catch (Exception e) {
                log.debug("No translation found for form {} in language {}", id, language);
            }

            // Generate preview HTML
            String html = templateGenerator.generatePreviewHtml(formDefinition, language, translation);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            log.error("Failed to generate preview for form {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("<html><body><h1>Error generating preview</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }

    /**
     * Generate field HTML only (without base template).
     * GET /api/forms/{id}/fields-html?lang=de
     *
     * @param id       Form definition ID
     * @param language Language code
     * @return Generated field HTML
     */
    @GetMapping(value = "/{id}/fields-html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generateFieldsHtml(
            @PathVariable UUID id,
            @RequestParam(value = "lang", defaultValue = "de") String language
    ) {
        log.info("Generating fields HTML for form={}, language={}", id, language);

        try {
            FormDefinition formDefinition = formDefinitionService.getFormById(id);

            // Load translation if available
            FormTranslationDTO translation = null;
            try {
                translation = formTranslationService.getTranslation(id, language);
            } catch (Exception e) {
                log.debug("No translation found for form {} in language {}", id, language);
            }

            // Generate fields only
            String fieldsHtml = templateGenerator.generateFieldsOnly(formDefinition, language, translation);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(fieldsHtml);

        } catch (Exception e) {
            log.error("Failed to generate fields for form {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("<!-- Error: " + e.getMessage() + " -->");
        }
    }

    /**
     * Validate form definition for template generation.
     * GET /api/forms/{id}/validate-template
     *
     * @param id Form definition ID
     * @return Validation result
     */
    @GetMapping("/{id}/validate-template")
    public ResponseEntity<Map<String, Object>> validateTemplate(@PathVariable UUID id) {
        log.info("Validating template generation for form={}", id);

        try {
            FormDefinition formDefinition = formDefinitionService.getFormById(id);

            ThymeleafTemplateGenerator.TemplateValidationResult result =
                    templateGenerator.validateFormDefinition(formDefinition);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to validate template for form {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("errors", java.util.List.of("Validation failed: " + e.getMessage()));
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Regenerate and clear cache for a form template.
     * POST /api/forms/{id}/regenerate-cache
     *
     * @param id Form definition ID
     * @return Success message
     */
    @PostMapping("/{id}/regenerate-cache")
    public ResponseEntity<Map<String, String>> regenerateCache(@PathVariable UUID id) {
        log.info("Regenerating cache for form={}", id);

        try {
            FormDefinition formDefinition = formDefinitionService.getFormById(id);

            // Clear cache
            if (cacheManager.getCache("thymeleafTemplates") != null) {
                // Clear all entries for this form (all languages)
                String[] languages = {"de", "en", "ar", "ru"};
                for (String lang : languages) {
                    String cacheKey = id + "_" + lang;
                    cacheManager.getCache("thymeleafTemplates").evict(cacheKey);
                    log.debug("Evicted cache entry: {}", cacheKey);
                }
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Cache regenerated successfully for form " + formDefinition.getName());
            response.put("formId", id.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to regenerate cache for form {}", id, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Cache regeneration failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
