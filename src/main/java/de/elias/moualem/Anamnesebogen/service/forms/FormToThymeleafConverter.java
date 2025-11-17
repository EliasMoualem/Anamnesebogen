package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Converts JSON Schema and UI Schema to Thymeleaf HTML field definitions.
 * This is the core mapping engine that transforms dynamic form definitions
 * into renderable Thymeleaf templates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormToThymeleafConverter {

    /**
     * Converts a complete form definition to Thymeleaf HTML fields.
     *
     * @param schema     JSON Schema defining structure and validation
     * @param uiSchema   UI Schema defining rendering hints
     * @param translation Optional translation for the given language
     * @return HTML string with Thymeleaf field definitions
     */
    public String convertToThymeleafFields(JsonNode schema, JsonNode uiSchema, FormTranslationDTO translation) {
        log.debug("Converting JSON Schema to Thymeleaf fields");

        if (schema == null || !schema.has("properties")) {
            log.warn("Schema is missing 'properties' node");
            return "";
        }

        JsonNode properties = schema.get("properties");
        JsonNode requiredFields = schema.get("required");
        Set<String> requiredSet = extractRequiredFields(requiredFields);

        // Get field order from UI Schema or use default order
        List<String> fieldOrder = extractFieldOrder(uiSchema, properties);

        StringBuilder html = new StringBuilder();
        html.append("<th:block>\n");

        for (String fieldName : fieldOrder) {
            if (!properties.has(fieldName)) {
                log.warn("Field '{}' in ui:order not found in schema properties", fieldName);
                continue;
            }

            JsonNode fieldSchema = properties.get(fieldName);
            JsonNode fieldUiSchema = uiSchema != null && uiSchema.has(fieldName) ? uiSchema.get(fieldName) : null;

            boolean isRequired = requiredSet.contains(fieldName);

            String fieldHtml = convertField(fieldName, fieldSchema, fieldUiSchema, isRequired, translation);
            html.append(fieldHtml);
        }

        html.append("</th:block>\n");

        return html.toString();
    }

    /**
     * Converts a single field to Thymeleaf HTML.
     */
    private String convertField(String fieldName, JsonNode fieldSchema, JsonNode fieldUiSchema,
                                 boolean isRequired, FormTranslationDTO translation) {

        String type = fieldSchema.has("type") ? fieldSchema.get("type").asText() : "string";
        String format = fieldSchema.has("format") ? fieldSchema.get("format").asText() : null;
        String widget = fieldUiSchema != null && fieldUiSchema.has("ui:widget")
                ? fieldUiSchema.get("ui:widget").asText()
                : null;

        // Get translated or default label
        String label = getFieldLabel(fieldName, fieldSchema, translation);
        String placeholder = getPlaceholder(fieldUiSchema, translation, fieldName);
        String helpText = getHelpText(fieldUiSchema, translation, fieldName);

        log.debug("Converting field: {} (type={}, format={}, widget={}, required={})",
                fieldName, type, format, widget, isRequired);

        return switch (type) {
            case "string" -> convertStringField(fieldName, label, fieldSchema, fieldUiSchema, format, widget,
                    isRequired, placeholder, helpText);
            case "integer", "number" -> convertNumberField(fieldName, label, fieldSchema, fieldUiSchema,
                    isRequired, placeholder, helpText);
            case "boolean" -> convertBooleanField(fieldName, label, helpText);
            default -> {
                log.warn("Unsupported field type '{}' for field '{}'", type, fieldName);
                yield "";
            }
        };
    }

    /**
     * Converts string-type fields (text, email, date, textarea, select, radio).
     */
    private String convertStringField(String fieldName, String label, JsonNode fieldSchema,
                                       JsonNode fieldUiSchema, String format, String widget,
                                       boolean isRequired, String placeholder, String helpText) {

        log.debug("convertStringField: fieldName={}, format={}, widget={}", fieldName, format, widget);

        // Check for enum (select or radio)
        if (fieldSchema.has("enum")) {
            return convertEnumField(fieldName, label, fieldSchema, widget, isRequired, helpText);
        }

        // Check for textarea widget
        if ("textarea".equals(widget)) {
            return convertTextareaField(fieldName, label, fieldSchema, fieldUiSchema, isRequired, placeholder, helpText);
        }

        // Check for format-specific fields
        if (format != null) {
            log.info("Field '{}' has format: '{}'", fieldName, format);
            return switch (format) {
                case "email" -> convertEmailField(fieldName, label, isRequired, placeholder, helpText);
                case "date" -> convertDateField(fieldName, label, fieldSchema, isRequired, helpText);
                case "uri", "url" -> convertUrlField(fieldName, label, isRequired, placeholder, helpText);
                case "signature" -> {
                    log.info("✅ Detected SIGNATURE format for field: {}", fieldName);
                    yield convertSignatureField(fieldName, label, isRequired, helpText);
                }
                default -> {
                    log.warn("Unknown format '{}' for field '{}'", format, fieldName);
                    yield convertTextField(fieldName, label, fieldSchema, isRequired, placeholder, helpText);
                }
            };
        }

        // Default to text field
        log.debug("Field '{}' has no format, defaulting to text field", fieldName);
        return convertTextField(fieldName, label, fieldSchema, isRequired, placeholder, helpText);
    }

    /**
     * Converts standard text input field.
     */
    private String convertTextField(String fieldName, String label, JsonNode fieldSchema,
                                     boolean isRequired, String placeholder, String helpText) {
        Integer minLength = fieldSchema.has("minLength") ? fieldSchema.get("minLength").asInt() : null;
        Integer maxLength = fieldSchema.has("maxLength") ? fieldSchema.get("maxLength").asInt() : null;
        String pattern = fieldSchema.has("pattern") ? fieldSchema.get("pattern").asText() : null;

        return String.format("""
                    <div class="form-group">
                        <label for="%s">%s%s</label>
                        <input type="text"
                               id="%s"
                               name="%s"
                               th:field="*{%s}"
                               %s
                               %s
                               %s
                               %s
                               class="form-control" />
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, label, isRequired ? "<span class='required'>*</span>" : "",
                fieldName, fieldName, fieldName,
                placeholder != null ? "placeholder=\"" + placeholder + "\"" : "",
                isRequired ? "required=\"required\"" : "",
                minLength != null ? "minlength=\"" + minLength + "\"" : "",
                maxLength != null ? "maxlength=\"" + maxLength + "\"" : "",
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts email input field.
     */
    private String convertEmailField(String fieldName, String label, boolean isRequired,
                                      String placeholder, String helpText) {
        return String.format("""
                    <div class="form-group">
                        <label for="%s">%s%s</label>
                        <input type="email"
                               id="%s"
                               name="%s"
                               th:field="*{%s}"
                               %s
                               %s
                               class="form-control" />
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, label, isRequired ? "<span class='required'>*</span>" : "",
                fieldName, fieldName, fieldName,
                placeholder != null ? "placeholder=\"" + placeholder + "\"" : "",
                isRequired ? "required=\"required\"" : "",
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts date input field.
     */
    private String convertDateField(String fieldName, String label, JsonNode fieldSchema,
                                     boolean isRequired, String helpText) {
        String min = fieldSchema.has("minimum") ? fieldSchema.get("minimum").asText() : null;
        String max = fieldSchema.has("maximum") ? fieldSchema.get("maximum").asText() : null;

        return String.format("""
                    <div class="form-group">
                        <label for="%s">%s%s</label>
                        <input type="date"
                               id="%s"
                               name="%s"
                               th:field="*{%s}"
                               %s
                               %s
                               %s
                               class="form-control" />
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, label, isRequired ? "<span class='required'>*</span>" : "",
                fieldName, fieldName, fieldName,
                isRequired ? "required=\"required\"" : "",
                min != null ? "min=\"" + min + "\"" : "",
                max != null ? "max=\"" + max + "\"" : "",
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts URL input field.
     */
    private String convertUrlField(String fieldName, String label, boolean isRequired,
                                    String placeholder, String helpText) {
        return String.format("""
                    <div class="form-group">
                        <label for="%s">%s%s</label>
                        <input type="url"
                               id="%s"
                               name="%s"
                               th:field="*{%s}"
                               %s
                               %s
                               class="form-control" />
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, label, isRequired ? "<span class='required'>*</span>" : "",
                fieldName, fieldName, fieldName,
                placeholder != null ? "placeholder=\"" + placeholder + "\"" : "",
                isRequired ? "required=\"required\"" : "",
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts textarea field.
     */
    private String convertTextareaField(String fieldName, String label, JsonNode fieldSchema,
                                         JsonNode fieldUiSchema, boolean isRequired,
                                         String placeholder, String helpText) {
        Integer rows = 3; // default
        if (fieldUiSchema != null && fieldUiSchema.has("ui:options")) {
            JsonNode options = fieldUiSchema.get("ui:options");
            if (options.has("rows")) {
                rows = options.get("rows").asInt();
            }
        }

        Integer minLength = fieldSchema.has("minLength") ? fieldSchema.get("minLength").asInt() : null;
        Integer maxLength = fieldSchema.has("maxLength") ? fieldSchema.get("maxLength").asInt() : null;

        return String.format("""
                    <div class="form-group">
                        <label for="%s">%s%s</label>
                        <textarea id="%s"
                                  name="%s"
                                  th:field="*{%s}"
                                  rows="%d"
                                  %s
                                  %s
                                  %s
                                  %s
                                  class="form-control"></textarea>
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, label, isRequired ? "<span class='required'>*</span>" : "",
                fieldName, fieldName, fieldName, rows,
                placeholder != null ? "placeholder=\"" + placeholder + "\"" : "",
                isRequired ? "required=\"required\"" : "",
                minLength != null ? "minlength=\"" + minLength + "\"" : "",
                maxLength != null ? "maxlength=\"" + maxLength + "\"" : "",
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts number/integer field.
     */
    private String convertNumberField(String fieldName, String label, JsonNode fieldSchema,
                                       JsonNode fieldUiSchema, boolean isRequired,
                                       String placeholder, String helpText) {
        Integer min = fieldSchema.has("minimum") ? fieldSchema.get("minimum").asInt() : null;
        Integer max = fieldSchema.has("maximum") ? fieldSchema.get("maximum").asInt() : null;
        Integer step = 1;

        return String.format("""
                    <div class="form-group">
                        <label for="%s">%s%s</label>
                        <input type="number"
                               id="%s"
                               name="%s"
                               th:field="*{%s}"
                               %s
                               %s
                               %s
                               %s
                               step="%d"
                               class="form-control" />
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, label, isRequired ? "<span class='required'>*</span>" : "",
                fieldName, fieldName, fieldName,
                placeholder != null ? "placeholder=\"" + placeholder + "\"" : "",
                isRequired ? "required=\"required\"" : "",
                min != null ? "min=\"" + min + "\"" : "",
                max != null ? "max=\"" + max + "\"" : "",
                step,
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts boolean/checkbox field.
     */
    private String convertBooleanField(String fieldName, String label, String helpText) {
        return String.format("""
                    <div class="form-group">
                        <div class="checkbox-item">
                            <input type="checkbox"
                                   id="%s"
                                   name="%s"
                                   th:field="*{%s}"
                                   value="true" />
                            <label for="%s">%s</label>
                        </div>
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, fieldName, fieldName,
                fieldName, label,
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts enum field (select dropdown or radio buttons).
     */
    private String convertEnumField(String fieldName, String label, JsonNode fieldSchema,
                                     String widget, boolean isRequired, String helpText) {
        List<Map<String, String>> options = extractEnumOptions(fieldSchema);

        if ("radio".equals(widget)) {
            return convertRadioField(fieldName, label, options, isRequired, helpText);
        } else {
            return convertSelectField(fieldName, label, options, isRequired, helpText);
        }
    }

    /**
     * Converts select dropdown field.
     */
    private String convertSelectField(String fieldName, String label, List<Map<String, String>> options,
                                       boolean isRequired, String helpText) {
        StringBuilder optionsHtml = new StringBuilder();
        optionsHtml.append("<option value=\"\">-- Select --</option>\n");

        for (Map<String, String> option : options) {
            optionsHtml.append(String.format(
                    "                <option value=\"%s\">%s</option>\n",
                    option.get("value"), option.get("label")
            ));
        }

        return String.format("""
                    <div class="form-group">
                        <label for="%s">%s%s</label>
                        <select id="%s"
                                name="%s"
                                th:field="*{%s}"
                                %s
                                class="form-control">
                %s        </select>
                        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                fieldName, label, isRequired ? "<span class='required'>*</span>" : "",
                fieldName, fieldName, fieldName,
                isRequired ? "required=\"required\"" : "",
                optionsHtml,
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    /**
     * Converts radio buttons field.
     */
    private String convertRadioField(String fieldName, String label, List<Map<String, String>> options,
                                      boolean isRequired, String helpText) {
        StringBuilder radioHtml = new StringBuilder();
        radioHtml.append("        <div class=\"radio-group\">\n");

        for (int i = 0; i < options.size(); i++) {
            Map<String, String> option = options.get(i);
            radioHtml.append(String.format("""
                            <div class="radio-item">
                                <input type="radio"
                                       id="%s_%d"
                                       name="%s"
                                       th:field="*{%s}"
                                       value="%s"
                                       %s />
                                <label for="%s_%d">%s</label>
                            </div>
                    """,
                    fieldName, i, fieldName, fieldName, option.get("value"),
                    isRequired ? "required=\"required\"" : "",
                    fieldName, i, option.get("label")
            ));
        }
        radioHtml.append("        </div>\n");

        return String.format("""
                    <div class="form-group">
                        <label>%s%s</label>
                %s        %s
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>
                """,
                label, isRequired ? "<span class='required'>*</span>" : "",
                radioHtml,
                helpText != null ? "<small class='help-text'>" + helpText + "</small>" : "",
                fieldName, fieldName
        );
    }

    // ========== HELPER METHODS ==========

    private Set<String> extractRequiredFields(JsonNode requiredFields) {
        if (requiredFields == null || !requiredFields.isArray()) {
            return Collections.emptySet();
        }

        Set<String> required = new HashSet<>();
        requiredFields.forEach(node -> required.add(node.asText()));
        return required;
    }

    private List<String> extractFieldOrder(JsonNode uiSchema, JsonNode properties) {
        if (uiSchema != null && uiSchema.has("ui:order")) {
            JsonNode orderNode = uiSchema.get("ui:order");
            List<String> order = new ArrayList<>();
            orderNode.forEach(node -> order.add(node.asText()));
            return order;
        }

        // Default order: schema properties order
        List<String> order = new ArrayList<>();
        properties.fieldNames().forEachRemaining(order::add);
        return order;
    }

    private String getFieldLabel(String fieldName, JsonNode fieldSchema, FormTranslationDTO translation) {
        // Try translation first
        if (translation != null && translation.getTranslations() != null) {
            JsonNode translations = translation.getTranslations();
            if (translations.has("fields") && translations.get("fields").has(fieldName)) {
                return translations.get("fields").get(fieldName).asText();
            }
        }

        // Fallback to schema title or field name
        if (fieldSchema.has("title")) {
            return fieldSchema.get("title").asText();
        }

        // Last resort: convert camelCase to Title Case
        return convertToTitleCase(fieldName);
    }

    private String getPlaceholder(JsonNode fieldUiSchema, FormTranslationDTO translation, String fieldName) {
        // Try translation
        if (translation != null && translation.getTranslations() != null) {
            JsonNode translations = translation.getTranslations();
            if (translations.has("placeholders") && translations.get("placeholders").has(fieldName)) {
                return translations.get("placeholders").get(fieldName).asText();
            }
        }

        // Fallback to UI schema
        if (fieldUiSchema != null && fieldUiSchema.has("ui:placeholder")) {
            return fieldUiSchema.get("ui:placeholder").asText();
        }

        return null;
    }

    private String getHelpText(JsonNode fieldUiSchema, FormTranslationDTO translation, String fieldName) {
        // Try translation
        if (translation != null && translation.getTranslations() != null) {
            JsonNode translations = translation.getTranslations();
            if (translations.has("helpTexts") && translations.get("helpTexts").has(fieldName)) {
                return translations.get("helpTexts").get(fieldName).asText();
            }
        }

        // Fallback to UI schema
        if (fieldUiSchema != null && fieldUiSchema.has("ui:help")) {
            return fieldUiSchema.get("ui:help").asText();
        }

        return null;
    }

    private List<Map<String, String>> extractEnumOptions(JsonNode fieldSchema) {
        if (!fieldSchema.has("enum")) {
            return Collections.emptyList();
        }

        JsonNode enumValues = fieldSchema.get("enum");
        JsonNode enumNames = fieldSchema.has("enumNames") ? fieldSchema.get("enumNames") : null;

        List<Map<String, String>> options = new ArrayList<>();
        for (int i = 0; i < enumValues.size(); i++) {
            String value = enumValues.get(i).asText();
            String label = enumNames != null && i < enumNames.size()
                    ? enumNames.get(i).asText()
                    : value;

            Map<String, String> option = new HashMap<>();
            option.put("value", value);
            option.put("label", label);
            options.add(option);
        }

        return options;
    }

    /**
     * Converts signature pad field.
     */
    private String convertSignatureField(String fieldName, String label, boolean isRequired, String helpText) {
        log.info("🖊️ Converting SIGNATURE field: {} (label={}, required={})", fieldName, label, isRequired);

        String html = String.format("""
                    <div class="form-group signature-group">
                        <label for="%s">%s%s</label>
                        %s
                        <div class="signature-container">
                            <canvas id="%s-canvas" class="signature-pad"></canvas>
                        </div>
                        <div class="signature-buttons">
                            <button type="button" class="btn-clear-signature" data-canvas="%s-canvas">Clear Signature</button>
                        </div>
                        <input type="hidden"
                               id="%s"
                               name="%s"
                               th:field="*{%s}"
                               %s
                               class="signature-data" />
                        <span th:if="${#fields.hasErrors('%s')}"
                              th:errors="*{%s}"
                              class="error-message"></span>
                    </div>

                """,
                fieldName,
                label,
                isRequired ? "<span class='required'>*</span>" : "",
                helpText != null ? "<span class='help-text'>" + helpText + "</span>" : "",
                fieldName,
                fieldName,
                fieldName,
                fieldName,
                fieldName,
                isRequired ? "required" : "",
                fieldName,
                fieldName
        );

        log.debug("Generated signature HTML: {}", html.substring(0, Math.min(200, html.length())) + "...");
        return html;
    }

    private String convertToTitleCase(String camelCase) {
        String result = camelCase.replaceAll("([A-Z])", " $1");
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
}
