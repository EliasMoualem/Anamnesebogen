package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.JsonNode;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for generating complete Thymeleaf HTML templates from FormDefinition entities.
 * Combines the base layout template with dynamically generated field HTML.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThymeleafTemplateGenerator {

    private final FormToThymeleafConverter converter;
    private final SpringTemplateEngine templateEngine;

    private static final String BASE_TEMPLATE_PATH = "src/main/resources/templates/base/dynamic-form-layout.html";
    private static final String FIELDS_FRAGMENT_PATH = "src/main/resources/templates/fragments/dynamic-form-fields.html";

    /**
     * Generates a complete Thymeleaf HTML template for the given form.
     * Result is cached per form ID and language.
     *
     * @param formDefinition The form definition to generate template for
     * @param language       Language code (de, en, ar, ru)
     * @param translation    Optional translation DTO
     * @return Complete HTML template as string
     */
    @Cacheable(value = "thymeleafTemplates", key = "#formDefinition.id + '_' + #language")
    public String generateTemplate(FormDefinition formDefinition, String language, FormTranslationDTO translation) {
        log.info("Generating Thymeleaf template for form '{}' (id={}, language={})",
                formDefinition.getName(), formDefinition.getId(), language);

        try {
            // Convert JSON Schema to field HTML
            String fieldsHtml = converter.convertToThymeleafFields(
                    formDefinition.getSchema(),
                    formDefinition.getUiSchema(),
                    translation
            );

            // Generate complete template by injecting fields into fragment
            String completeTemplate = generateCompleteTemplate(fieldsHtml);

            log.debug("Generated template for form '{}': {} characters",
                    formDefinition.getName(), completeTemplate.length());

            return completeTemplate;

        } catch (Exception e) {
            log.error("Failed to generate template for form '{}'", formDefinition.getName(), e);
            throw new RuntimeException("Template generation failed", e);
        }
    }

    /**
     * Generates a preview HTML (for admin UI) without data binding.
     *
     * @param formDefinition The form definition
     * @param language       Language code
     * @param translation    Translation DTO
     * @return Preview HTML
     */
    public String generatePreviewHtml(FormDefinition formDefinition, String language, FormTranslationDTO translation) {
        log.debug("Generating preview HTML for form '{}'", formDefinition.getName());

        try {
            // Generate field HTML
            String fieldsHtml = converter.convertToThymeleafFields(
                    formDefinition.getSchema(),
                    formDefinition.getUiSchema(),
                    translation
            );

            // Generate complete standalone HTML
            return generateStandaloneHtml(formDefinition, fieldsHtml, language);

        } catch (Exception e) {
            log.error("Failed to generate preview HTML for form '{}'", formDefinition.getName(), e);
            return generateErrorHtml("Preview generation failed: " + e.getMessage());
        }
    }

    /**
     * Generates a complete standalone HTML document for preview.
     */
    private String generateStandaloneHtml(FormDefinition formDefinition, String fieldsHtml, String language) {
        boolean isRtl = "ar".equalsIgnoreCase(language);

        return String.format("""
<!DOCTYPE html>
<html lang="%s" dir="%s">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>%s</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            min-height: 100vh;
            padding: 20px;
        }
        .form-container {
            max-width: 800px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        .form-header {
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .form-header h1 {
            font-size: 28px;
            margin-bottom: 10px;
            font-weight: 600;
        }
        .form-header p {
            font-size: 16px;
            opacity: 0.9;
        }
        .form-body {
            padding: 40px;
        }
        .form-group {
            margin-bottom: 25px;
        }
        .form-group label {
            display: block;
            font-weight: 600;
            margin-bottom: 8px;
            color: #333;
            font-size: 14px;
        }
        .form-group label .required {
            color: #e74c3c;
            margin-left: 4px;
        }
        .form-group input[type="text"],
        .form-group input[type="email"],
        .form-group input[type="tel"],
        .form-group input[type="number"],
        .form-group input[type="date"],
        .form-group select,
        .form-group textarea {
            width: 100%%;
            padding: 12px 15px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 15px;
            transition: all 0.3s ease;
            font-family: inherit;
        }
        .form-group input:focus,
        .form-group select:focus,
        .form-group textarea:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }
        .form-group textarea {
            resize: vertical;
            min-height: 100px;
        }
        .form-group input[type="checkbox"],
        .form-group input[type="radio"] {
            margin-right: 8px;
            width: 18px;
            height: 18px;
            cursor: pointer;
        }
        .form-group .checkbox-group,
        .form-group .radio-group {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }
        .form-group .checkbox-item,
        .form-group .radio-item {
            display: flex;
            align-items: center;
            padding: 10px;
            background: #f8f9fa;
            border-radius: 6px;
            cursor: pointer;
        }
        .form-group .help-text {
            display: block;
            font-size: 13px;
            color: #6c757d;
            margin-top: 6px;
            font-style: italic;
        }
        .error-message {
            color: #e74c3c;
            font-size: 13px;
            margin-top: 6px;
            display: block;
            font-weight: 500;
        }
        .form-actions {
            display: flex;
            gap: 15px;
            justify-content: flex-end;
            margin-top: 30px;
            padding-top: 30px;
            border-top: 2px solid #f0f0f0;
        }
        .btn {
            padding: 14px 32px;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
        }
        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            color: white;
        }
        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
        }
        .btn-secondary {
            background: #6c757d;
            color: white;
        }
        @media (max-width: 768px) {
            .form-body { padding: 25px; }
            .form-header { padding: 20px; }
            .form-header h1 { font-size: 22px; }
        }
    </style>
</head>
<body>
    <div class="form-container">
        <div class="form-header">
            <h1>%s</h1>
            %s
        </div>
        <div class="form-body">
            <form method="post" action="#">
                %s
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary">Cancel</button>
                    <button type="submit" class="btn btn-primary">Submit</button>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
                """,
                language,
                isRtl ? "rtl" : "ltr",
                formDefinition.getName(),
                formDefinition.getName(),
                formDefinition.getDescription() != null
                    ? "<p>" + formDefinition.getDescription() + "</p>"
                    : "",
                fieldsHtml
        );
    }

    /**
     * Generates an error HTML page.
     */
    private String generateErrorHtml(String errorMessage) {
        return String.format("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Preview Error</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            padding: 40px;
            background: #f5f5f5;
        }
        .error-container {
            max-width: 600px;
            margin: 0 auto;
            background: white;
            padding: 40px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        h1 { color: #e74c3c; }
        pre {
            background: #f8f8f8;
            padding: 15px;
            border-radius: 4px;
            overflow-x: auto;
        }
    </style>
</head>
<body>
    <div class="error-container">
        <h1>Preview Generation Error</h1>
        <p>An error occurred while generating the preview:</p>
        <pre>%s</pre>
    </div>
</body>
</html>
                """,
                errorMessage
        );
    }

    /**
     * Generates raw field HTML only (without base template).
     * Useful for debugging or API responses.
     *
     * @param formDefinition The form definition
     * @param language       Language code
     * @param translation    Translation DTO
     * @return Field HTML only
     */
    public String generateFieldsOnly(FormDefinition formDefinition, String language, FormTranslationDTO translation) {
        log.debug("Generating fields-only HTML for form '{}'", formDefinition.getName());

        return converter.convertToThymeleafFields(
                formDefinition.getSchema(),
                formDefinition.getUiSchema(),
                translation
        );
    }

    /**
     * Validates that a form definition can be converted to Thymeleaf.
     *
     * @param formDefinition The form definition to validate
     * @return Validation result with errors if any
     */
    public TemplateValidationResult validateFormDefinition(FormDefinition formDefinition) {
        log.debug("Validating form definition for template generation: {}", formDefinition.getName());

        TemplateValidationResult result = new TemplateValidationResult();

        // Check for required fields
        if (formDefinition.getSchema() == null) {
            result.addError("Schema is null");
        } else {
            JsonNode schema = formDefinition.getSchema();

            if (!schema.has("properties")) {
                result.addError("Schema missing 'properties' node");
            }

            if (!schema.has("type") || !"object".equals(schema.get("type").asText())) {
                result.addError("Schema type must be 'object'");
            }
        }

        if (formDefinition.getUiSchema() == null) {
            result.addWarning("UI Schema is null - using defaults");
        }

        // Try to generate to catch any conversion errors
        try {
            converter.convertToThymeleafFields(
                    formDefinition.getSchema(),
                    formDefinition.getUiSchema(),
                    null
            );
        } catch (Exception e) {
            result.addError("Conversion failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Determines if the language uses right-to-left text direction.
     */
    private boolean isRightToLeft(String language) {
        return "ar".equalsIgnoreCase(language); // Arabic
    }

    /**
     * Generates complete template by reading the base template file and injecting field HTML.
     * Used by generateTemplate() method.
     *
     * @param fieldsHtml The generated field HTML to inject
     * @return Complete template with fields injected
     */
    private String generateCompleteTemplate(String fieldsHtml) {
        try {
            // Read the fragment template
            Path fragmentPath = Paths.get(FIELDS_FRAGMENT_PATH);
            String fragmentTemplate = Files.readString(fragmentPath);

            // Replace placeholder with actual fields
            return fragmentTemplate.replace("<!-- FIELDS_PLACEHOLDER -->", fieldsHtml);

        } catch (IOException e) {
            log.error("Failed to read template file: {}", FIELDS_FRAGMENT_PATH, e);
            throw new RuntimeException("Failed to generate complete template", e);
        }
    }

    /**
     * Result object for template validation.
     */
    @Getter
    public static class TemplateValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        @Override
        public String toString() {
            if (isValid()) {
                return "Valid" + (warnings.isEmpty() ? "" : " (with warnings: " + warnings + ")");
            }
            return "Invalid - Errors: " + errors;
        }
    }
}
