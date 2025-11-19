package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for validating form data against JSON Schema definitions.
 * Uses the NetworkNT JSON Schema Validator library to ensure submitted
 * data conforms to the schema defined in FormDefinition.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormValidationService {

    private final ObjectMapper objectMapper;

    /**
     * Validates form data against a JSON Schema.
     *
     * @param formData Map of field names to values from submitted form
     * @param schema   JSON Schema to validate against
     * @return ValidationResult containing validation status and error messages
     */
    public ValidationResult validateFormData(Map<String, Object> formData, JsonNode schema) {
        log.debug("Validating form data against schema");

        try {
            // Convert form data Map to JsonNode
            JsonNode dataNode = objectMapper.valueToTree(formData);

            // Create JSON Schema validator
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema jsonSchema = factory.getSchema(schema);

            // Validate
            Set<ValidationMessage> validationMessages = jsonSchema.validate(dataNode);

            if (validationMessages.isEmpty()) {
                log.debug("Form data validation passed");
                return ValidationResult.success();
            } else {
                // Convert validation messages to user-friendly errors
                Map<String, List<String>> fieldErrors = new HashMap<>();
                List<String> globalErrors = new ArrayList<>();

                for (ValidationMessage msg : validationMessages) {
                    String path = msg.getInstanceLocation().toString();
                    String message = msg.getMessage();

                    // Extract field name from JSON path (e.g., "$.firstName" -> "firstName")
                    String fieldName = extractFieldName(path);

                    if (fieldName != null && !fieldName.isEmpty()) {
                        fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(message);
                    } else {
                        globalErrors.add(message);
                    }

                    log.debug("Validation error: path={}, message={}", path, message);
                }

                log.warn("Form data validation failed with {} error(s)", validationMessages.size());
                return ValidationResult.failure(fieldErrors, globalErrors);
            }

        } catch (Exception e) {
            log.error("Failed to validate form data", e);
            return ValidationResult.failure(
                    Collections.emptyMap(),
                    Collections.singletonList("Validation error: " + e.getMessage())
            );
        }
    }

    /**
     * Extracts field name from JSON path.
     * Examples:
     * - "$.firstName" -> "firstName"
     * - "/firstName" -> "firstName"
     * - "$.address.street" -> "address.street"
     * - "$" or "/" or "" -> null
     */
    private String extractFieldName(String path) {
        if (path == null || path.isEmpty() || path.equals("$") || path.equals("/")) {
            return null;
        }

        // Remove leading "$." if present
        if (path.startsWith("$.")) {
            return path.substring(2);
        }

        // Remove leading "/" and replace "/" with "."
        if (path.startsWith("/")) {
            String fieldPath = path.substring(1);
            return fieldPath.replace("/", ".");
        }

        return path;
    }

    /**
     * Result of form validation.
     */
    @Getter
    public static class ValidationResult {
        private final boolean valid;
        private final Map<String, List<String>> fieldErrors;
        private final List<String> globalErrors;

        private ValidationResult(boolean valid, Map<String, List<String>> fieldErrors, List<String> globalErrors) {
            this.valid = valid;
            this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
            this.globalErrors = globalErrors != null ? globalErrors : Collections.emptyList();
        }

        public static ValidationResult success() {
            return new ValidationResult(true, Collections.emptyMap(), Collections.emptyList());
        }

        public static ValidationResult failure(Map<String, List<String>> fieldErrors, List<String> globalErrors) {
            return new ValidationResult(false, fieldErrors, globalErrors);
        }

        public List<String> getAllErrors() {
            List<String> allErrors = new ArrayList<>(globalErrors);
            fieldErrors.forEach((field, errors) -> {
                for (String error : errors) {
                    allErrors.add(field + ": " + error);
                }
            });
            return allErrors;
        }

        public String getFirstError() {
            if (!globalErrors.isEmpty()) {
                return globalErrors.getFirst();
            }
            if (!fieldErrors.isEmpty()) {
                Map.Entry<String, List<String>> firstEntry = fieldErrors.entrySet().iterator().next();
                return firstEntry.getKey() + ": " + firstEntry.getValue().getFirst();
            }
            return null;
        }

        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult{valid=true}";
            }
            return "ValidationResult{valid=false, errors=" + getAllErrors() + "}";
        }
    }
}
