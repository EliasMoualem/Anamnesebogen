package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.elias.moualem.Anamnesebogen.entity.*;
import de.elias.moualem.Anamnesebogen.repository.FormSubmissionRepository;
import de.elias.moualem.Anamnesebogen.repository.PatientRepository;
import de.elias.moualem.Anamnesebogen.repository.SignatureRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling dynamic form submissions.
 * Maps dynamic form data to Patient entities and creates FormSubmission records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormSubmissionService {

    private final PatientRepository patientRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final SignatureRepository signatureRepository;
    private final FormDefinitionService formDefinitionService;
    private final ObjectMapper objectMapper;

    /**
     * Process and save a form submission.
     * Creates or updates Patient record and saves FormSubmission with data snapshot.
     *
     * @param formId        UUID of the form definition
     * @param formData      Map of field names to values from the submitted form
     * @param language      Language code of the submission
     * @param request       HTTP request for metadata (IP, user agent)
     * @return Created FormSubmission entity
     */
    @Transactional
    public FormSubmission processSubmission(UUID formId, Map<String, Object> formData,
                                             String language, HttpServletRequest request) {
        log.info("Processing form submission for form {}, language={}", formId, language);

        try {
            // Load form definition
            FormDefinition formDefinition = formDefinitionService.getFormById(formId);

            // Extract and create/update patient
            Patient patient = extractAndSavePatient(formData, language);

            // Create FormSubmission entity
            FormSubmission submission = FormSubmission.builder()
                    .formDefinition(formDefinition)
                    .patient(patient)
                    .submissionDate(LocalDateTime.now())
                    .formLanguage(language)
                    .formVersion(formDefinition.getVersion())
                    .ipAddress(extractIpAddress(request))
                    .userAgent(extractUserAgent(request))
                    .deviceType(detectDeviceType(request))
                    .status(FormSubmission.SubmissionStatus.SUBMITTED)
                    .formDataSnapshot(convertToJsonNode(formData))
                    .build();

            // Save submission
            FormSubmission savedSubmission = formSubmissionRepository.save(submission);

            log.info("Form submission saved successfully: id={}, patient={}, form={}",
                    savedSubmission.getId(), patient.getId(), formDefinition.getName());

            // Process signatures (if any)
            processSignatures(savedSubmission, patient, formData, request);

            return savedSubmission;

        } catch (Exception e) {
            log.error("Failed to process form submission for form {}", formId, e);
            throw new RuntimeException("Form submission processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract patient data from dynamic form fields and create/update Patient entity.
     * Maps common field names to Patient entity fields.
     *
     * Expected field names (case-insensitive):
     * - firstName, first_name, vorname
     * - lastName, last_name, nachname, familienname
     * - birthDate, birth_date, geburtsdatum, dateOfBirth
     * - email, emailAddress, email_address, e_mail
     * - phone, phoneNumber, telefon, telefonnummer
     * - mobile, mobileNumber, mobilnummer, handy
     * - street, strasse, address
     * - zipCode, zip_code, plz, postalCode
     * - city, stadt, ort
     * - gender, geschlecht
     */
    private Patient extractAndSavePatient(Map<String, Object> formData, String language) {
        log.debug("Extracting patient data from form fields: {}", formData.keySet());

        // Extract required fields with fallbacks
        String firstName = extractField(formData, "firstName", "first_name", "vorname");
        String lastName = extractField(formData, "lastName", "last_name", "nachname", "familienname");
        String birthDateStr = extractField(formData, "birthDate", "birth_date", "geburtsdatum", "dateOfBirth");

        // Validate required fields
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required but not found in form data");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required but not found in form data");
        }
        if (birthDateStr == null || birthDateStr.isBlank()) {
            throw new IllegalArgumentException("Birth date is required but not found in form data");
        }

        // Parse birthdate
        LocalDate birthDate = parseBirthDate(birthDateStr);

        // Extract optional fields
        String email = extractField(formData, "email", "emailAddress", "email_address", "e_mail");
        String phone = extractField(formData, "phone", "phoneNumber", "telefon", "telefonnummer");
        String mobile = extractField(formData, "mobile", "mobileNumber", "mobilnummer", "handy");
        String street = extractField(formData, "street", "strasse", "address");
        String zipCode = extractField(formData, "zipCode", "zip_code", "plz", "postalCode");
        String city = extractField(formData, "city", "stadt", "ort");
        String gender = extractField(formData, "gender", "geschlecht");

        // Check if patient exists (by email or name+birthdate combination)
        Optional<Patient> existingPatient = findExistingPatient(email, firstName, lastName, birthDate);

        if (existingPatient.isPresent()) {
            log.info("Found existing patient: {}", existingPatient.get().getId());
            Patient patient = existingPatient.get();
            // Update patient data with latest submission
            updatePatientData(patient, formData, language, email, phone, mobile, street, zipCode, city, gender);
            return patientRepository.save(patient);
        } else {
            log.info("Creating new patient: {} {}", firstName, lastName);
            // Create new patient
            Patient newPatient = Patient.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .birthDate(birthDate)
                    .emailAddress(email)
                    .phoneNumber(phone)
                    .mobileNumber(mobile)
                    .street(street)
                    .zipCode(zipCode)
                    .city(city)
                    .gender(gender)
                    .language(language)
                    .insuranceType(InsuranceType.SELF_INSURED)  // TODO: Default, can be updated later
                    .build();

            return patientRepository.save(newPatient);
        }
    }

    /**
     * Extract field value by trying multiple possible field name variations.
     */
    private String extractField(Map<String, Object> formData, String... possibleNames) {
        for (String name : possibleNames) {
            // Try exact match
            if (formData.containsKey(name)) {
                Object value = formData.get(name);
                return value != null ? value.toString().trim() : null;
            }
            // Try case-insensitive match
            for (Map.Entry<String, Object> entry : formData.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    Object value = entry.getValue();
                    return value != null ? value.toString().trim() : null;
                }
            }
        }
        return null;
    }

    /**
     * Parse birthdate from string with multiple format support.
     */
    private LocalDate parseBirthDate(String birthDateStr) {
        // Try multiple date formats
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE,           // 2000-01-31
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),  // 31.01.2000
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),  // 31/01/2000
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),  // 01/31/2000
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),  // 2000-01-31
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(birthDateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        throw new IllegalArgumentException("Invalid birth date format: " + birthDateStr +
                ". Expected formats: YYYY-MM-DD, DD.MM.YYYY, DD/MM/YYYY");
    }

    /**
     * Find existing patient by email or name+birthdate combination.
     */
    private Optional<Patient> findExistingPatient(String email, String firstName,
                                                    String lastName, LocalDate birthDate) {
        // First try to find by email (most reliable)
        // Returns the most recently created patient if duplicates exist
        if (email != null && !email.isBlank()) {
            var byEmail = patientRepository.findByEmailAddress(email);
            if (!byEmail.isEmpty()) {
                if (byEmail.size() > 1) {
                    log.warn("Found {} duplicate patients with email '{}'. Using most recent (id={})",
                            byEmail.size(), email, byEmail.getFirst().getId());
                }
                return Optional.of(byEmail.getFirst());
            }
        }

        // Fallback: find by name and birthdate
        // Returns the most recently created patient if duplicates exist
        var patients = patientRepository.findByFirstNameAndLastNameAndBirthDate(firstName, lastName, birthDate);
        if (!patients.isEmpty()) {
            if (patients.size() > 1) {
                log.warn("Found {} duplicate patients with name '{} {}' and birthdate '{}'. Using most recent (id={})",
                        patients.size(), firstName, lastName, birthDate, patients.getFirst().getId());
            }
            return Optional.of(patients.get(0));
        }

        return Optional.empty();
    }

    /**
     * Update existing patient data with latest submission.
     */
    private void updatePatientData(Patient patient, Map<String, Object> formData, String language,
                                    String email, String phone, String mobile, String street,
                                    String zipCode, String city, String gender) {
        // Update only non-null fields
        if (email != null && !email.isBlank()) {
            patient.setEmailAddress(email);
        }
        if (phone != null && !phone.isBlank()) {
            patient.setPhoneNumber(phone);
        }
        if (mobile != null && !mobile.isBlank()) {
            patient.setMobileNumber(mobile);
        }
        if (street != null && !street.isBlank()) {
            patient.setStreet(street);
        }
        if (zipCode != null && !zipCode.isBlank()) {
            patient.setZipCode(zipCode);
        }
        if (city != null && !city.isBlank()) {
            patient.setCity(city);
        }
        if (gender != null && !gender.isBlank()) {
            patient.setGender(gender);
        }
        if (language != null && !language.isBlank()) {
            patient.setLanguage(language);
        }

        log.debug("Updated patient {} with latest data", patient.getId());
    }

    /**
     * Convert Map to JsonNode for storage.
     */
    private JsonNode convertToJsonNode(Map<String, Object> formData) {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        formData.forEach((key, value) -> {
            if (value != null) {
                jsonNode.put(key, value.toString());
            }
        });
        return jsonNode;
    }

    /**
     * Extract IP address from request.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Extract user agent from request.
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }

    /**
     * Detect device type from user agent.
     */
    private String detectDeviceType(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    /**
     * Retrieve a form submission by ID.
     */
    @Transactional(readOnly = true)
    public FormSubmission getSubmissionById(UUID submissionId) {
        return formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Form submission not found: " + submissionId));
    }

    /**
     * Mark submission as completed with PDF generated.
     */
    @Transactional
    public void markSubmissionCompleted(UUID submissionId, String pdfPath, String pdfHash) {
        FormSubmission submission = getSubmissionById(submissionId);
        submission.markCompleted(pdfPath, pdfHash);
        formSubmissionRepository.save(submission);
        log.info("Marked submission {} as completed", submissionId);
    }

    /**
     * Mark submission as failed.
     */
    @Transactional
    public void markSubmissionFailed(UUID submissionId, String errorMessage) {
        FormSubmission submission = getSubmissionById(submissionId);
        submission.markFailed(errorMessage);
        formSubmissionRepository.save(submission);
        log.error("Marked submission {} as failed: {}", submissionId, errorMessage);
    }

    /**
     * Process signature fields from form data and create Signature entities.
     */
    private void processSignatures(FormSubmission submission, Patient patient,
                                     Map<String, Object> formData, HttpServletRequest request) {
        log.debug("Processing signatures for submission {}", submission.getId());

        formData.forEach((fieldName, value) -> {
            // Check if value is a base64 PNG signature
            if (value instanceof String && ((String) value).startsWith("data:image/png;base64,")) {
                log.info("Found signature field: {}", fieldName);

                try {
                    // Extract base64 data (remove "data:image/png;base64," prefix)
                    String base64Data = ((String) value).split(",")[1];
                    byte[] signatureBytes = Base64.getDecoder().decode(base64Data);

                    // Calculate SHA-256 hash
                    String hash = calculateSHA256Hash(signatureBytes);

                    // Create Signature entity
                    Signature signature = Signature.builder()
                            .signatureData(signatureBytes)
                            .signatureHash(hash)
                            .signatureMimeType("image/png")
                            .signatureType(Signature.SignatureType.SIMPLE)
                            .documentType(Signature.DocumentType.ANAMNESIS)
                            .signerName(patient.getFirstName() + " " + patient.getLastName())
                            .ipAddress(extractIpAddress(request))
                            .userAgent(extractUserAgent(request))
                            .deviceInfo(detectDeviceType(request))
                            .patient(patient)
                            .formSubmission(submission)
                            .identityConfirmed(false)
                            .build();

                    // Save signature
                    Signature savedSignature = signatureRepository.save(signature);
                    log.info("Signature saved: id={}, fieldName={}, hash={}, size={} bytes",
                            savedSignature.getId(), fieldName, hash, signatureBytes.length);

                } catch (Exception e) {
                    log.error("Failed to process signature field '{}': {}", fieldName, e.getMessage(), e);
                    // Don't fail the entire submission if signature processing fails
                }
            }
        });
    }

    /**
     * Calculate SHA-256 hash of byte array.
     */
    private String calculateSHA256Hash(byte[] data) {
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
}
