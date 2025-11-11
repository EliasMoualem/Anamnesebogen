package de.elias.moualem.Anamnesebogen.service;

import de.elias.moualem.Anamnesebogen.dto.GuardianDTO;
import de.elias.moualem.Anamnesebogen.dto.PatientFormDTO;
import de.elias.moualem.Anamnesebogen.dto.PolicyholderDTO;
import de.elias.moualem.Anamnesebogen.entity.*;
import de.elias.moualem.Anamnesebogen.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing patient data and database operations.
 * Handles conversion between legacy PatientFormDTO model and new Patient entity.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PolicyholderRepository policyholderRepository;
    private final LegalGuardianRepository legalGuardianRepository;
    private final SignatureRepository signatureRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final ConsentRepository consentRepository;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    /**
     * Save patient data from form submission to database.
     * Determines insurance type and creates policyholder if needed.
     *
     * @param patientForm Form data
     * @param ipAddress Submission IP address
     * @param userAgent Submission user agent
     * @return Saved patient UUID
     */
    @Transactional
    public UUID savePatient(PatientFormDTO patientForm, String ipAddress, String userAgent) {
        log.info("Saving patient: {} {}", patientForm.getFirstName(), patientForm.getLastName());

        // Convert PatientFormDTO to Patient entity
        Patient patient = convertToEntity(patientForm);
        patient.setSubmissionIpAddress(ipAddress);
        patient.setSubmissionUserAgent(userAgent);

        // Determine insurance type based on mainInsuranceParent presence
        boolean hasPolicyholder = patientForm.getPolicyholder() != null
                && patientForm.getPolicyholder().getFirstName() != null
                && !patientForm.getPolicyholder().getFirstName().isEmpty();

        if (hasPolicyholder) {
            patient.setInsuranceType(InsuranceType.FAMILY_INSURED);
            log.info("Patient is FAMILY_INSURED, policyholder will be created");
        } else {
            patient.setInsuranceType(InsuranceType.SELF_INSURED);
            log.info("Patient is SELF_INSURED");
        }

        // Grant data processing consent
        patient.grantConsent();

        // Calculate data retention date (10 years from now)
        patient.calculateRetentionDate();

        // Save patient first
        Patient savedPatient = patientRepository.save(patient);
        log.info("Patient saved with ID: {} (Age: {}, Minor: {})",
                savedPatient.getId(), savedPatient.getAge(), savedPatient.isMinor());

        // Create guardian if patient is a minor
        boolean hasGuardian = patientForm.getGuardian() != null
                && patientForm.getGuardian().hasData();
        if (hasGuardian) {
            createGuardian(savedPatient, patientForm.getGuardian());
        }

        // Create policyholder if FAMILY_INSURED
        if (hasPolicyholder) {
            createPolicyholder(savedPatient, patientForm.getPolicyholder());
        }

        // Validate business rules after all relationships are created
        try {
            savedPatient.validate();
        } catch (IllegalStateException e) {
            log.error("Patient validation failed: {}", e.getMessage());
            throw e;
        }

        // Save signature if present
        if (patientForm.hasSignature()) {
            saveSignature(savedPatient, patientForm.getSignature());
        }

        // Create form submission record
        createFormSubmission(savedPatient, patientForm.getLanguage());

        // Create consent record
        createConsentRecord(savedPatient);

        return savedPatient.getId();
    }

    /**
     * Get patient by ID and convert to PatientFormDTO for PDF generation.
     *
     * @param patientId Patient UUID
     * @return PatientFormDTO model or empty if not found
     */
    @Transactional
    public Optional<PatientFormDTO> getPatientForPdf(UUID patientId) {
        log.info("Retrieving patient for PDF: {}", patientId);

        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (patientOpt.isEmpty()) {
            log.warn("Patient not found: {}", patientId);
            return Optional.empty();
        }

        Patient patient = patientOpt.get();
        PatientFormDTO patientForm = convertToPatientFormDTO(patient);

        // Get signature if available
        signatureRepository.findLatestByPatientId(patientId).ifPresent(signature -> {
            patientForm.setSignature(signature.getSignatureData());
            patientForm.setSignatureId(signature.getId());
            patientForm.setSignatureTimestamp(signature.getSignedAt());
            log.info("Loaded signature for patient with ID: {} at {}", signature.getId(), signature.getSignedAt());
        });

        // Get guardian if patient is a minor
        if (patient.isMinor()) {
            legalGuardianRepository.findByPatientId(patientId).ifPresent(guardian -> {
                GuardianDTO guardianDTO = convertToGuardianDTO(guardian);
                patientForm.setGuardian(guardianDTO);
                log.info("Loaded guardian for minor patient: {} ({})",
                        guardian.getFullName(), guardian.getRelationshipType());
            });
        }

        // Get policyholder if FAMILY_INSURED
        if (patient.isFamilyInsured()) {
            policyholderRepository.findByPatientId(patientId).ifPresent(policyholder -> {
                PolicyholderDTO parent = convertToPolicyholderDTO(policyholder);
                patientForm.setPolicyholder(parent);
                log.info("Loaded policyholder for FAMILY_INSURED patient");
            });
        }

        return Optional.of(patientForm);
    }

    /**
     * Convert PatientFormDTO form model to Patient entity.
     */
    private Patient convertToEntity(PatientFormDTO patientForm) {
        Patient patient = Patient.builder()
                .firstName(patientForm.getFirstName())
                .lastName(patientForm.getLastName())
                .birthDate(parseDate(patientForm.getBirthDate()))
                .gender(patientForm.getGender())
                .street(patientForm.getStreet())
                .zipCode(patientForm.getZipCode())
                .city(patientForm.getCity())
                .mobileNumber(patientForm.getMobileNumber())
                .phoneNumber(patientForm.getPhoneNumber())
                .emailAddress(patientForm.getEmailAddress())
                .language(patientForm.getLanguage())
                .insuranceProvider(patientForm.getInsuranceProvider())
                .insurancePolicyNumber(patientForm.getInsurancePolicyNumber())
                .insuranceGroupNumber(patientForm.getInsuranceGroupNumber())
                .policyholderName(patientForm.getPolicyholderName())
                .relationshipToPolicyholder(patientForm.getRelationshipToPolicyholder())
                .allergies(patientForm.getAllergies())
                .currentMedications(patientForm.getCurrentMedications())
                .medicalConditions(patientForm.getMedicalConditions())
                .previousSurgeries(patientForm.getPreviousSurgeries())
                .primaryCareDoctor(patientForm.getPrimaryCareDoctor())
                .build();

        return patient;
    }

    /**
     * Convert Patient entity back to PatientFormDTO for PDF generation.
     */
    private PatientFormDTO convertToPatientFormDTO(Patient patient) {
        PatientFormDTO patientForm = new PatientFormDTO();
        patientForm.setFirstName(patient.getFirstName());
        patientForm.setLastName(patient.getLastName());
        patientForm.setBirthDate(formatDate(patient.getBirthDate()));
        patientForm.setGender(patient.getGender());
        patientForm.setStreet(patient.getStreet());
        patientForm.setZipCode(patient.getZipCode());
        patientForm.setCity(patient.getCity());
        patientForm.setMobileNumber(patient.getMobileNumber());
        patientForm.setPhoneNumber(patient.getPhoneNumber());
        patientForm.setEmailAddress(patient.getEmailAddress());
        patientForm.setLanguage(patient.getLanguage());
        patientForm.setInsuranceProvider(patient.getInsuranceProvider());
        patientForm.setInsurancePolicyNumber(patient.getInsurancePolicyNumber());
        patientForm.setInsuranceGroupNumber(patient.getInsuranceGroupNumber());
        patientForm.setPolicyholderName(patient.getPolicyholderName());
        patientForm.setRelationshipToPolicyholder(patient.getRelationshipToPolicyholder());
        patientForm.setAllergies(patient.getAllergies());
        patientForm.setCurrentMedications(patient.getCurrentMedications());
        patientForm.setMedicalConditions(patient.getMedicalConditions());
        patientForm.setPreviousSurgeries(patient.getPreviousSurgeries());
        patientForm.setPrimaryCareDoctor(patient.getPrimaryCareDoctor());

        return patientForm;
    }

    /**
     * Convert Policyholder entity to PolicyholderDTO model.
     */
    private PolicyholderDTO convertToPolicyholderDTO(Policyholder policyholder) {
        PolicyholderDTO parent = new PolicyholderDTO();
        parent.setFirstName(policyholder.getFirstName());
        parent.setLastName(policyholder.getLastName());
        parent.setBirthDate(formatDate(policyholder.getBirthDate()));
        parent.setGender(policyholder.getGender());
        parent.setStreet(policyholder.getStreet());
        parent.setZipCode(policyholder.getZipCode());
        parent.setCity(policyholder.getCity());
        parent.setMobileNumber(policyholder.getMobileNumber());
        parent.setPhoneNumber(policyholder.getPhoneNumber());
        parent.setEmailAddress(policyholder.getEmailAddress());
        parent.setJob(policyholder.getJob());

        return parent;
    }

    /**
     * Convert LegalGuardian entity to GuardianDTO.
     */
    private GuardianDTO convertToGuardianDTO(LegalGuardian guardian) {
        return GuardianDTO.builder()
                .firstName(guardian.getFirstName())
                .lastName(guardian.getLastName())
                .birthDate(formatDate(guardian.getBirthDate()))
                .gender(guardian.getGender())
                .street(guardian.getStreet())
                .zipCode(guardian.getZipCode())
                .city(guardian.getCity())
                .mobileNumber(guardian.getMobileNumber())
                .phoneNumber(guardian.getPhoneNumber())
                .emailAddress(guardian.getEmailAddress())
                .job(guardian.getJob())
                .relationshipType(guardian.getRelationshipType().name())
                .build();
    }

    /**
     * Create and save policyholder for FAMILY_INSURED patient.
     */
    private void createPolicyholder(Patient patient, PolicyholderDTO mainInsuranceParent) {
        Policyholder policyholder = Policyholder.builder()
                .patient(patient)
                .firstName(mainInsuranceParent.getFirstName())
                .lastName(mainInsuranceParent.getLastName())
                .birthDate(parseDate(mainInsuranceParent.getBirthDate()))
                .gender(mainInsuranceParent.getGender())
                .street(mainInsuranceParent.getStreet())
                .zipCode(mainInsuranceParent.getZipCode())
                .city(mainInsuranceParent.getCity())
                .mobileNumber(mainInsuranceParent.getMobileNumber())
                .phoneNumber(mainInsuranceParent.getPhoneNumber())
                .emailAddress(mainInsuranceParent.getEmailAddress())
                .job(mainInsuranceParent.getJob())
                .build();

        policyholderRepository.save(policyholder);
        patient.setPolicyholder(policyholder); // Set bidirectional relationship
        log.info("Policyholder created for patient: {}", patient.getId());
    }

    /**
     * Create and save legal guardian for MINOR patient.
     * Business Rule: Only called for patients under 18 years old.
     */
    private void createGuardian(Patient patient, GuardianDTO guardianDTO) {
        // Parse relationship type from string to enum
        LegalGuardian.RelationshipType relationshipType;
        try {
            relationshipType = LegalGuardian.RelationshipType.valueOf(guardianDTO.getRelationshipType().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid relationship type: {}, defaulting to OTHER", guardianDTO.getRelationshipType());
            relationshipType = LegalGuardian.RelationshipType.OTHER;
        }

        LegalGuardian guardian = LegalGuardian.builder()
                .patient(patient)
                .firstName(guardianDTO.getFirstName())
                .lastName(guardianDTO.getLastName())
                .birthDate(parseDate(guardianDTO.getBirthDate()))
                .gender(guardianDTO.getGender())
                .street(guardianDTO.getStreet())
                .zipCode(guardianDTO.getZipCode())
                .city(guardianDTO.getCity())
                .mobileNumber(guardianDTO.getMobileNumber())
                .phoneNumber(guardianDTO.getPhoneNumber())
                .emailAddress(guardianDTO.getEmailAddress())
                .job(guardianDTO.getJob())
                .relationshipType(relationshipType)
                .build();

        legalGuardianRepository.save(guardian);
        patient.setGuardian(guardian); // Set bidirectional relationship
        log.info("Legal guardian created for minor patient: {} - Guardian: {} ({})",
                patient.getId(), guardian.getFullName(), relationshipType);
    }

    /**
     * Create and save signature record.
     */
    private void saveSignature(Patient patient, byte[] signatureData) {
        // Calculate SHA-256 hash of signature data
        String signatureHash = calculateSHA256Hash(signatureData);

        // Get full name for signature
        String signerName = patient.getFirstName() + " " + patient.getLastName();

        Signature signature = Signature.builder()
                .patient(patient)
                .signatureData(signatureData)
                .signatureHash(signatureHash)
                .signatureType(Signature.SignatureType.SIMPLE)
                .documentType(Signature.DocumentType.ANAMNESIS)
                .signerName(signerName)
                .ipAddress(patient.getSubmissionIpAddress())
                .userAgent(patient.getSubmissionUserAgent())
                .signedAt(LocalDateTime.now())
                .build();

        signatureRepository.save(signature);
        log.info("Signature saved for patient: {} ({}) with hash: {}", patient.getId(), signerName, signatureHash);
    }

    /**
     * Calculate SHA-256 hash of byte array.
     * Used for signature tamper detection.
     */
    private String calculateSHA256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to calculate signature hash", e);
        }
    }

    /**
     * Create form submission record.
     */
    private void createFormSubmission(Patient patient, String language) {
        FormSubmission submission = FormSubmission.builder()
                .patient(patient)
                .submissionDate(LocalDateTime.now())
                .formLanguage(language != null ? language : "de")
                .status(FormSubmission.SubmissionStatus.SUBMITTED)
                .build();

        formSubmissionRepository.save(submission);
        log.info("Form submission record created for patient: {}", patient.getId());
    }

    /**
     * Create data processing consent record.
     */
    private void createConsentRecord(Patient patient) {
        Consent consent = Consent.builder()
                .patient(patient)
                .consentType(Consent.ConsentType.DATA_PROCESSING)
                .status(Consent.ConsentStatus.GRANTED)
                .consentDate(LocalDateTime.now())
                .consentVersion("1.0")
                .consentText("Patient has consented to data processing for medical treatment purposes")
                .captureMethod(Consent.CaptureMethod.WEB_FORM)
                .proofType(Consent.ProofType.CHECKBOX)
                .ipAddress(patient.getSubmissionIpAddress())
                .userAgent(patient.getSubmissionUserAgent())
                .build();

        consentRepository.save(consent);
        log.info("Consent record created for patient: {}", patient.getId());
    }

    /**
     * Parse date string to LocalDate with multiple format support.
     */
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        log.warn("Could not parse date: {}", dateString);
        return null;
    }

    /**
     * Format LocalDate to string for display.
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
