package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FormValidationService.
 */
@ExtendWith(MockitoExtension.class)
class FormValidationServiceTest {

    private FormValidationService validationService;
    private ObjectMapper objectMapper;

    @Mock
    private FieldTypeService fieldTypeService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validationService = new FormValidationService(objectMapper, fieldTypeService);
    }

    @Test
    void testValidationSuccess_AllFieldsValid() throws Exception {
        // Given: A schema with required fields and constraints
        String schemaJson = """
            {
              "type": "object",
              "required": ["firstName", "lastName", "email"],
              "properties": {
                "firstName": {
                  "type": "string",
                  "minLength": 2,
                  "maxLength": 50
                },
                "lastName": {
                  "type": "string",
                  "minLength": 2,
                  "maxLength": 50
                },
                "email": {
                  "type": "string",
                  "format": "email"
                },
                "age": {
                  "type": "integer",
                  "minimum": 0,
                  "maximum": 150
                }
              }
            }
            """;
        JsonNode schema = objectMapper.readTree(schemaJson);

        // And: Valid form data
        Map<String, Object> formData = new HashMap<>();
        formData.put("firstName", "John");
        formData.put("lastName", "Doe");
        formData.put("email", "john.doe@example.com");
        formData.put("age", 30);

        // When: Validating
        FormValidationService.ValidationResult result = validationService.validateFormData(formData, schema);

        // Then: Validation should pass
        assertThat(result.isValid()).isTrue();
        assertThat(result.getFieldErrors()).isEmpty();
        assertThat(result.getGlobalErrors()).isEmpty();
    }

    @Test
    void testValidationFailure_MissingRequiredField() throws Exception {
        // Given: A schema with required fields
        String schemaJson = """
            {
              "type": "object",
              "required": ["firstName", "lastName", "email"],
              "properties": {
                "firstName": { "type": "string" },
                "lastName": { "type": "string" },
                "email": { "type": "string" }
              }
            }
            """;
        JsonNode schema = objectMapper.readTree(schemaJson);

        // And: Form data missing required field
        Map<String, Object> formData = new HashMap<>();
        formData.put("firstName", "John");
        // Missing: lastName and email

        // When: Validating
        FormValidationService.ValidationResult result = validationService.validateFormData(formData, schema);

        // Then: Validation should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAllErrors()).isNotEmpty();
    }

    @Test
    void testValidationFailure_StringTooShort() throws Exception {
        // Given: A schema with minLength constraint
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "firstName": {
                  "type": "string",
                  "minLength": 2
                }
              }
            }
            """;
        JsonNode schema = objectMapper.readTree(schemaJson);

        // And: Form data with string too short
        Map<String, Object> formData = new HashMap<>();
        formData.put("firstName", "J"); // Only 1 character, but minimum is 2

        // When: Validating
        FormValidationService.ValidationResult result = validationService.validateFormData(formData, schema);

        // Then: Validation should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFieldErrors()).containsKey("firstName");
    }

    @Test
    void testValidationFailure_NumberOutOfRange() throws Exception {
        // Given: A schema with minimum/maximum constraints
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "age": {
                  "type": "integer",
                  "minimum": 0,
                  "maximum": 150
                }
              }
            }
            """;
        JsonNode schema = objectMapper.readTree(schemaJson);

        // And: Form data with number out of range
        Map<String, Object> formData = new HashMap<>();
        formData.put("age", 200); // Exceeds maximum of 150

        // When: Validating
        FormValidationService.ValidationResult result = validationService.validateFormData(formData, schema);

        // Then: Validation should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFieldErrors()).containsKey("age");
    }

    @Test
    void testValidationFailure_InvalidEmail() throws Exception {
        // Given: A schema with email format
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "email": {
                  "type": "string",
                  "format": "email"
                }
              }
            }
            """;
        JsonNode schema = objectMapper.readTree(schemaJson);

        // And: Form data with invalid email
        Map<String, Object> formData = new HashMap<>();
        formData.put("email", "not-an-email");

        // When: Validating
        FormValidationService.ValidationResult result = validationService.validateFormData(formData, schema);

        // Then: Validation should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFieldErrors()).containsKey("email");
    }

    @Test
    void testValidationSuccess_WithSignatureField() throws Exception {
        // Given: A schema with signature field
        String schemaJson = """
            {
              "type": "object",
              "required": ["firstName", "patientSignature"],
              "properties": {
                "firstName": {
                  "type": "string"
                },
                "patientSignature": {
                  "type": "string",
                  "format": "signature"
                }
              }
            }
            """;
        JsonNode schema = objectMapper.readTree(schemaJson);

        // And: Valid form data with signature (base64 PNG)
        Map<String, Object> formData = new HashMap<>();
        formData.put("firstName", "John");
        formData.put("patientSignature", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

        // When: Validating
        FormValidationService.ValidationResult result = validationService.validateFormData(formData, schema);

        // Then: Validation should pass (signature format is not strictly validated by JSON Schema)
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidationResult_GetFirstError() throws Exception {
        // Given: A schema with required fields
        String schemaJson = """
            {
              "type": "object",
              "required": ["firstName", "lastName"],
              "properties": {
                "firstName": { "type": "string" },
                "lastName": { "type": "string" }
              }
            }
            """;
        JsonNode schema = objectMapper.readTree(schemaJson);

        // And: Form data missing required fields
        Map<String, Object> formData = new HashMap<>();

        // When: Validating
        FormValidationService.ValidationResult result = validationService.validateFormData(formData, schema);

        // Then: Should have first error
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFirstError()).isNotNull();
    }

    @Test
    void testValidationResult_ToString() {
        // Given: A successful validation result
        FormValidationService.ValidationResult successResult = FormValidationService.ValidationResult.success();

        // Then: toString should indicate success
        assertThat(successResult.toString()).contains("valid=true");

        // Given: A failed validation result
        Map<String, java.util.List<String>> errors = new HashMap<>();
        errors.put("firstName", java.util.List.of("is required"));
        FormValidationService.ValidationResult failureResult =
                FormValidationService.ValidationResult.failure(errors, java.util.List.of());

        // Then: toString should show errors
        assertThat(failureResult.toString()).contains("valid=false");
        assertThat(failureResult.toString()).contains("errors");
    }
}
