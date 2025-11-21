package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormSubmission;
import de.elias.moualem.Anamnesebogen.entity.Patient;
import de.elias.moualem.Anamnesebogen.entity.Signature;
import de.elias.moualem.Anamnesebogen.repository.SignatureRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating PDFs from dynamic form submissions.
 * Converts JSON Schema-based form data into formatted PDF documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicFormPdfService {

    private final FormDefinitionService formDefinitionService;
    private final FormTranslationService translationService;
    private final FormSubmissionService submissionService;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final SignatureRepository signatureRepository;

    /**
     * Generate PDF from form submission data.
     *
     * @param formId       Form definition ID
     * @param submissionData Form submission data as JSON
     * @param language     Language code
     * @return PDF file
     * @throws Exception If PDF generation fails
     */
    public File generatePdfFromSubmission(UUID formId, Map<String, Object> submissionData, String language) throws Exception {
        log.info("Generating PDF for form={}, language={}", formId, language);

        // Load form definition
        FormDefinition formDefinition = formDefinitionService.getFormById(formId);

        // Load translation
        FormTranslationDTO translation = null;
        try {
            translation = translationService.getTranslation(formId, language);
        } catch (Exception e) {
            log.warn("No translation found for form {} in language {}", formId, language);
        }

        // Generate HTML content
        String html = generatePdfHtml(formDefinition, submissionData, translation, language);

        // Create temporary PDF file
        Path tempFile = Files.createTempFile("form_" + formId + "_", ".pdf");
        File pdfFile = tempFile.toFile();

        // Convert HTML to PDF
        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
        }

        log.info("PDF generated: {}", pdfFile.getAbsolutePath());
        return pdfFile;
    }

    /**
     * Generate PDF from FormSubmission entity.
     * This is the primary method for generating PDFs from saved submissions.
     *
     * @param submissionId UUID of the form submission
     * @return PDF file
     * @throws Exception If PDF generation fails
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public File generatePdfFromSubmissionEntity(UUID submissionId) throws Exception {
        log.info("Generating PDF for submission={}", submissionId);

        // Load submission
        FormSubmission submission = submissionService.getSubmissionById(submissionId);
        FormDefinition formDefinition = submission.getFormDefinition();
        Patient patient = submission.getPatient();

        // Eagerly load patient fields to avoid lazy loading issues
        if (patient != null) {
            patient.getFirstName();
            patient.getLastName();
            patient.getBirthDate();
            patient.getEmailAddress();
            patient.getPhoneNumber();
            patient.getStreet();
            patient.getCity();
            patient.getZipCode();
        }

        // Convert JsonNode to Map
        Map<String, Object> submissionData = jsonNodeToMap(submission.getFormDataSnapshot());

        // Load translation
        FormTranslationDTO translation = null;
        try {
            translation = translationService.getTranslation(
                    formDefinition.getId(),
                    submission.getFormLanguage()
            );
        } catch (Exception e) {
            log.warn("No translation found for form {} in language {}",
                    formDefinition.getId(), submission.getFormLanguage());
        }

        // Generate HTML with patient info
        String html = generatePdfHtmlWithPatient(
                formDefinition,
                patient,
                submissionData,
                translation,
                submission.getFormLanguage(),
                submission.getSubmissionDate()
        );

        // Create temporary PDF file
        Path tempFile = Files.createTempFile("submission_" + submissionId + "_", ".pdf");
        File pdfFile = tempFile.toFile();

        // Convert HTML to PDF
        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
        }

        log.info("PDF generated: {}", pdfFile.getAbsolutePath());

        // Calculate PDF hash
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
        String pdfHash = calculateSHA256(pdfBytes);

        // Update submission with PDF metadata
        submissionService.markSubmissionCompleted(submissionId, pdfFile.getAbsolutePath(), pdfHash);

        return pdfFile;
    }

    /**
     * Generate PDF and serve it from a FormSubmission entity.
     *
     * @param submissionId Form submission ID
     * @param response     HTTP response
     * @throws Exception If PDF generation fails
     */
    public void generateAndServePdfFromSubmission(UUID submissionId, HttpServletResponse response) throws Exception {
        log.info("Generating and serving PDF for submission={}", submissionId);

        // Generate PDF file
        File pdfFile = generatePdfFromSubmissionEntity(submissionId);
        Path file = pdfFile.toPath();

        if (Files.exists(file)) {
            // Load submission for filename
            FormSubmission submission = submissionService.getSubmissionById(submissionId);
            FormDefinition formDefinition = submission.getFormDefinition();
            Patient patient = submission.getPatient();

            String sanitizedFormName = formDefinition.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
            String patientName = (patient != null && patient.getLastName() != null && patient.getFirstName() != null)
                ? patient.getLastName() + "_" + patient.getFirstName()
                : "patient";
            String sanitizedPatientName = patientName.replaceAll("[^a-zA-Z0-9-_]", "_");
            String timestamp = submission.getSubmissionDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String filename = String.format("%s_%s_%s.pdf",
                    sanitizedFormName,
                    sanitizedPatientName,
                    timestamp);

            // Set response headers
            response.setContentType("application/pdf");
            response.addHeader("Content-Disposition", "attachment; filename=" + filename);

            // Copy file to response output stream
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();

            log.info("PDF written to response: {}", filename);

            // Clean up temp file
            try {
                Files.delete(file);
                log.debug("Deleted temporary PDF file: {}", file);
            } catch (Exception e) {
                log.warn("Failed to delete temporary PDF file: {}", file, e);
            }
        } else {
            log.error("Generated PDF file does not exist: {}", file);
            throw new java.io.IOException("Generated PDF file not found: " + file);
        }
    }

    /**
     * Generates HTML for PDF rendering from form definition and submission data.
     *
     * @param formDefinition Form definition entity
     * @param submissionData Form submission data
     * @param translation    Translation DTO (optional)
     * @param language       Language code
     * @return HTML string ready for PDF conversion
     */
    private String generatePdfHtml(FormDefinition formDefinition, Map<String, Object> submissionData,
                                    FormTranslationDTO translation, String language) {
        log.debug("Generating PDF HTML for form: {}", formDefinition.getName());

        // Create Thymeleaf context
        Context context = new Context();
        context.setVariable("formDefinition", formDefinition);
        context.setVariable("submissionData", submissionData);
        context.setVariable("isRtl", "ar".equalsIgnoreCase(language));
        context.setVariable("language", language);
        context.setVariable("generatedAt", LocalDateTime.now());

        // Add formatted submission data for display
        Map<String, String> formattedData = formatSubmissionData(formDefinition, submissionData, translation);
        context.setVariable("formattedData", formattedData);

        // Load logo if available
        try {
            byte[] logoBytes = getClass().getClassLoader()
                    .getResourceAsStream("templates/logo.PNG")
                    .readAllBytes();
            String base64Logo = Base64.getEncoder().encodeToString(logoBytes);
            context.setVariable("logoImage", base64Logo);
            log.debug("Logo image loaded for PDF");
        } catch (Exception e) {
            log.warn("Could not load logo image for PDF", e);
        }

        // Process template - use dedicated PDF template for dynamic forms
        return templateEngine.process("pdf_dynamic_form", context);
    }

    /**
     * Formats submission data for PDF display.
     * Converts raw form values into human-readable format with labels.
     *
     * @param formDefinition Form definition
     * @param submissionData Raw submission data
     * @param translation    Translation (optional)
     * @return Formatted data map (fieldName -> formatted value)
     */
    private Map<String, String> formatSubmissionData(FormDefinition formDefinition,
                                                      Map<String, Object> submissionData,
                                                      FormTranslationDTO translation) {
        // Use LinkedHashMap to preserve insertion order
        Map<String, String> formatted = new LinkedHashMap<>();

        JsonNode schema = formDefinition.getSchema();
        if (schema == null || !schema.has("properties")) {
            return formatted;
        }

        JsonNode properties = schema.get("properties");
        JsonNode uiSchema = formDefinition.getUiSchema();

        // Extract field order from UI Schema (respects ui:order from form builder)
        List<String> fieldOrder = extractFieldOrder(uiSchema, properties);

        // Iterate fields in the specified order
        for (String fieldName : fieldOrder) {
            // Try to get schema - handle field names with/without spaces
            JsonNode fieldSchema = getFieldSchema(properties, fieldName);

            if (fieldSchema == null) {
                log.warn("No schema found for field '{}', skipping", fieldName);
                continue;
            }

            // Skip signature fields - they are displayed separately
            String format = fieldSchema.has("format") ? fieldSchema.get("format").asText() : null;
            if ("signature".equals(format)) {
                continue;
            }

            // Get value using case-insensitive lookup
            Object value = getCaseInsensitiveValue(submissionData, fieldName);

            // Get field label
            String label = getFieldLabel(fieldName, fieldSchema, translation);

            // Format value based on type (handles null values)
            String formattedValue = formatValue(value, fieldSchema);

            log.debug("Formatting field '{}': label='{}', value='{}', formatted='{}'",
                fieldName, label, value, formattedValue);

            formatted.put(fieldName, label + ": " + formattedValue);
        }

        return formatted;
    }

    /**
     * Gets field label from schema or translation.
     */
    private String getFieldLabel(String fieldName, JsonNode fieldSchema, FormTranslationDTO translation) {
        // Try translation first
        if (translation != null && translation.getTranslations() != null) {
            JsonNode translations = translation.getTranslations();
            if (translations.has("fields") && translations.get("fields").has(fieldName)) {
                return translations.get("fields").get(fieldName).asText();
            }
        }

        // Fallback to schema title
        if (fieldSchema.has("title")) {
            return fieldSchema.get("title").asText();
        }

        // Last resort: convert camelCase to Title Case
        return fieldName.replaceAll("([A-Z])", " $1").trim();
    }

    /**
     * Formats a value based on its field type.
     */
    private String formatValue(Object value, JsonNode fieldSchema) {
        if (value == null) {
            return "-";
        }

        String type = fieldSchema.has("type") ? fieldSchema.get("type").asText() : "string";
        String format = fieldSchema.has("format") ? fieldSchema.get("format").asText() : null;

        // Handle different types
        switch (type) {
            case "boolean":
                // Handle both actual booleans and string representations
                boolean boolValue;
                if (value instanceof Boolean) {
                    boolValue = (Boolean) value;
                } else if (value instanceof String) {
                    // Fallback for existing submissions with string "true"/"false"
                    String strValue = ((String) value).toLowerCase();
                    boolValue = "true".equals(strValue) || "yes".equals(strValue) || "1".equals(strValue);
                } else {
                    boolValue = Boolean.TRUE.equals(value);
                }
                return boolValue ? "Yes" : "No";
            case "array":
                return value.toString().replace("[", "").replace("]", "");
            default:
                // Check for enum (select/radio)
                if (fieldSchema.has("enum") && fieldSchema.has("enumNames")) {
                    return formatEnumValue(value.toString(), fieldSchema);
                }
                return value.toString();
        }
    }

    /**
     * Formats enum value by looking up the display name.
     */
    private String formatEnumValue(String value, JsonNode fieldSchema) {
        JsonNode enumValues = fieldSchema.get("enum");
        JsonNode enumNames = fieldSchema.get("enumNames");

        for (int i = 0; i < enumValues.size(); i++) {
            if (enumValues.get(i).asText().equals(value)) {
                if (enumNames != null && i < enumNames.size()) {
                    return enumNames.get(i).asText();
                }
                return value;
            }
        }

        return value;
    }

    /**
     * Generates HTML for PDF with patient information.
     */
    private String generatePdfHtmlWithPatient(FormDefinition formDefinition, Patient patient,
                                               Map<String, Object> submissionData,
                                               FormTranslationDTO translation, String language,
                                               LocalDateTime submissionDate) {
        log.debug("Generating PDF HTML with patient info for form: {}", formDefinition.getName());

        // Create Thymeleaf context
        Context context = new Context();
        context.setVariable("formDefinition", formDefinition);
        context.setVariable("patient", patient);
        context.setVariable("submissionData", submissionData);
        context.setVariable("submissionDate", submissionDate);
        context.setVariable("isRtl", "ar".equalsIgnoreCase(language));
        context.setVariable("language", language);
        context.setVariable("generatedAt", LocalDateTime.now());

        // Merge patient data into submission data (ensures patient fields are included)
        Map<String, Object> mergedData = new HashMap<>(submissionData);
        if (patient != null) {
            mergePatientData(mergedData, patient);
        }

        // Add formatted submission data for display
        Map<String, String> formattedData = formatSubmissionData(formDefinition, mergedData, translation);
        context.setVariable("formattedData", formattedData);

        // Load logo if available
        try {
            byte[] logoBytes = getClass().getClassLoader()
                    .getResourceAsStream("templates/logo.PNG")
                    .readAllBytes();
            String base64Logo = Base64.getEncoder().encodeToString(logoBytes);
            context.setVariable("logoImage", base64Logo);
            log.debug("Logo image loaded for PDF");
        } catch (Exception e) {
            log.warn("Could not load logo image for PDF", e);
        }

        // Load signatures for this submission (find by patient and approximate timestamp)
        List<Signature> signatures = signatureRepository.findByPatientId(patient.getId());
        // Filter signatures close to submission time (within 1 hour before/after)
        List<Signature> relevantSignatures = signatures.stream()
                .filter(sig -> sig.getSignedAt() != null &&
                        sig.getSignedAt().isAfter(submissionDate.minusHours(1)) &&
                        sig.getSignedAt().isBefore(submissionDate.plusHours(1)))
                .toList();

        // Prepare signature data for template
        Map<String, Map<String, Object>> signatureMap = new HashMap<>();
        for (Signature sig : relevantSignatures) {
            Map<String, Object> sigData = new HashMap<>();
            sigData.put("base64Image", Base64.getEncoder().encodeToString(sig.getSignatureData()));
            sigData.put("uuid", sig.getId().toString());
            sigData.put("signedAt", sig.getSignedAt());
            sigData.put("signerName", sig.getSignerName());
            sigData.put("hash", sig.getSignatureHash());
            signatureMap.put("signature_" + sig.getId(), sigData);
        }

        context.setVariable("signatures", signatureMap);
        log.debug("Loaded {} signatures for PDF", signatureMap.size());

        // Process template
        return templateEngine.process("pdf_dynamic_form", context);
    }

    /**
     * Converts JsonNode to Map.
     */
    private Map<String, Object> jsonNodeToMap(JsonNode jsonNode) {
        Map<String, Object> map = new HashMap<>();
        if (jsonNode != null && jsonNode.isObject()) {
            jsonNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    map.put(key, value.asText());
                } else if (value.isNumber()) {
                    map.put(key, value.asDouble());
                } else if (value.isBoolean()) {
                    map.put(key, value.asBoolean());
                } else {
                    map.put(key, value.toString());
                }
            });
        }
        return map;
    }

    /**
     * Calculate SHA-256 hash of byte array.
     */
    private String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to calculate SHA-256 hash", e);
            return null;
        }
    }

    /**
     * Extracts field order from UI Schema.
     * Uses ui:order if present, otherwise falls back to schema properties order.
     *
     * @param uiSchema   UI Schema (may contain ui:order)
     * @param properties Schema properties
     * @return Ordered list of field names
     */
    private List<String> extractFieldOrder(JsonNode uiSchema, JsonNode properties) {
        // Try to get field order from UI Schema
        if (uiSchema != null && uiSchema.has("ui:order")) {
            JsonNode orderNode = uiSchema.get("ui:order");
            List<String> order = new ArrayList<>();
            orderNode.forEach(node -> order.add(node.asText().trim())); // Trim whitespace
            log.debug("Using field order from ui:order: {}", order);
            return order;
        }

        // Fallback: use properties iteration order
        List<String> order = new ArrayList<>();
        properties.fieldNames().forEachRemaining(name -> order.add(name.trim())); // Trim whitespace
        log.debug("Using default field order from properties: {}", order);
        return order;
    }

    /**
     * Gets field schema from properties using flexible matching (handles whitespace).
     *
     * @param properties JsonNode containing field schemas
     * @param fieldName  Field name to find
     * @return Field schema if found, null otherwise
     */
    private JsonNode getFieldSchema(JsonNode properties, String fieldName) {
        // Try exact match first
        if (properties.has(fieldName)) {
            return properties.get(fieldName);
        }

        // Try with/without spaces - iterate all property names
        String trimmedName = fieldName.trim();
        var iterator = properties.fields();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String propertyName = entry.getKey().trim();
            if (propertyName.equals(trimmedName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Gets a value from the map using case-insensitive key lookup.
     *
     * @param map The map to search
     * @param key The key to find (case-insensitive)
     * @return The value if found, null otherwise
     */
    private Object getCaseInsensitiveValue(Map<String, Object> map, String key) {
        // Try exact match first (fast path)
        if (map.containsKey(key)) {
            return map.get(key);
        }

        // Try case-insensitive match
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Merges patient entity data into submission data map.
     * Ensures patient fields (firstName, lastName, etc.) are available for PDF generation.
     * Uses case-insensitive field name matching to handle variations.
     *
     * @param submissionData The submission data map to merge into
     * @param patient        The patient entity with correct data
     */
    private void mergePatientData(Map<String, Object> submissionData, Patient patient) {
        if (patient == null) {
            log.warn("Patient is null, cannot merge patient data");
            return;
        }

        log.debug("Merging patient data: firstName={}, lastName={}, birthDate={}",
            patient.getFirstName(), patient.getLastName(), patient.getBirthDate());
        log.debug("Submission data keys before merge: {}", submissionData.keySet());

        // Merge patient fields - try multiple variations of field names
        mergeField(submissionData, patient.getFirstName(), "firstName", "first_name", "firstname", "vorname");
        mergeField(submissionData, patient.getLastName(), "lastName", "last_name", "lastname", "nachname", "familienname");
        mergeField(submissionData, patient.getBirthDate(), "birthDate", "birth_date", "birthdate", "geburtsdatum", "dateOfBirth");
        mergeField(submissionData, patient.getEmailAddress(), "email", "emailAddress", "email_address", "e-mail");
        mergeField(submissionData, patient.getPhoneNumber(), "phone", "phoneNumber", "phone_number", "telefon");
        mergeField(submissionData, patient.getStreet(), "street", "strasse", "address");
        mergeField(submissionData, patient.getCity(), "city", "stadt");
        mergeField(submissionData, patient.getZipCode(), "zipCode", "zip_code", "zip", "plz", "postalCode");

        log.debug("Submission data keys after merge: {}", submissionData.keySet());
        log.debug("Merged patient data into submission data");
    }

    /**
     * Merges a single field value into the submission data using case-insensitive matching.
     * Tries multiple field name variations and sets the first match found.
     *
     * @param submissionData The map to merge into
     * @param value          The value to set
     * @param fieldNames     Possible field name variations (tried in order)
     */
    private void mergeField(Map<String, Object> submissionData, Object value, String... fieldNames) {
        if (value == null) {
            return;
        }

        // Try to find any existing field with a matching name (case-insensitive)
        for (String fieldName : fieldNames) {
            for (String existingKey : submissionData.keySet()) {
                if (existingKey.equalsIgnoreCase(fieldName)) {
                    // Found a match - ALWAYS overwrite with patient entity data (source of truth)
                    submissionData.put(existingKey, value);
                    log.debug("Merged field '{}' with value '{}' from patient entity", existingKey, value);
                    return;
                }
            }
        }

        // If no existing field found, add using the first field name variation
        submissionData.put(fieldNames[0], value);
        log.debug("Added new field '{}' with value '{}' from patient entity", fieldNames[0], value);
    }
}
