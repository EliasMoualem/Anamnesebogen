package de.elias.moualem.Anamnesebogen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Patient entity representing a patient in the dental practice.
 * Contains GDPR-compliant fields for consent management and data retention.
 */
@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patient_email", columnList = "email_address"),
    @Index(name = "idx_patient_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ===================================
    // Personal Information (PII - should be encrypted)
    // ===================================

    @NotNull
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotEmpty
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotNull
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Email
    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "language", length = 10)
    private String language;

    // ===================================
    // Insurance Information
    // ===================================

    /**
     * Type of insurance coverage.
     * - SELF_INSURED: Patient has their own insurance policy (Selbstversichert)
     * - FAMILY_INSURED: Patient is covered under parent/guardian policy (Familienversichert)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type", length = 20, nullable = false)
    @Builder.Default
    private InsuranceType insuranceType = InsuranceType.SELF_INSURED;

    /**
     * Insurance provider name.
     * For SELF_INSURED: patient's own insurance company.
     * For FAMILY_INSURED: policyholder's (guardian's) insurance company.
     */
    @Column(name = "insurance_provider", length = 255)
    private String insuranceProvider;

    @Column(name = "insurance_policy_number", length = 100)
    private String insurancePolicyNumber;

    @Column(name = "insurance_group_number", length = 100)
    private String insuranceGroupNumber;

    @Column(name = "policyholder_name", length = 200)
    private String policyholderName;

    @Column(name = "relationship_to_policyholder", length = 50)
    private String relationshipToPolicyholder;

    // ===================================
    // Medical History
    // ===================================

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "current_medications", columnDefinition = "TEXT")
    private String currentMedications;

    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    @Column(name = "previous_surgeries", columnDefinition = "TEXT")
    private String previousSurgeries;

    @Column(name = "primary_care_doctor", length = 255)
    private String primaryCareDoctor;

    // ===================================
    // GDPR Compliance Fields
    // ===================================

    /**
     * Tracks if patient has consented to data processing
     */
    @Column(name = "data_processing_consent", nullable = false)
    @Builder.Default
    private Boolean dataProcessingConsent = false;

    /**
     * When the patient consented to data processing
     */
    @Column(name = "data_processing_consent_date")
    private LocalDateTime dataProcessingConsentDate;

    /**
     * Date when patient data should be deleted (based on retention policy)
     * German law: 10 years for medical records
     */
    @Column(name = "data_retention_until")
    private LocalDate dataRetentionUntil;

    /**
     * Tracks if patient has requested data deletion (Right to be forgotten)
     */
    @Column(name = "deletion_requested")
    @Builder.Default
    private Boolean deletionRequested = false;

    /**
     * When the deletion was requested
     */
    @Column(name = "deletion_requested_date")
    private LocalDateTime deletionRequestedDate;

    /**
     * IP address from which the form was submitted (for audit purposes)
     */
    @Column(name = "submission_ip_address", length = 45)
    private String submissionIpAddress;

    /**
     * User agent string from form submission
     */
    @Column(name = "submission_user_agent", length = 500)
    private String submissionUserAgent;

    // ===================================
    // Dynamic Form Fields
    // ===================================

    /**
     * Custom fields from dynamic form builder stored as JSONB.
     * Allows flexible form fields without schema changes.
     *
     * Structure:
     * {
     *   "hasAllergies": true,
     *   "allergyDetails": "Pollen und Nüsse",
     *   "wearsBraces": false,
     *   "previousOrthodontics": {
     *     "hasPreviousTreatment": true,
     *     "treatmentYear": "2018"
     *   },
     *   "customQuestion_12345": "Answer to custom question"
     * }
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private com.fasterxml.jackson.databind.JsonNode customFields;

    // ===================================
    // Relationships
    // ===================================

    /**
     * Policyholder information for FAMILY_INSURED patients.
     * This is the Hauptversicherter (main policyholder) under whose insurance
     * this patient (mitversicherte Person) is covered. Typically a parent for minor patients.
     * Only populated if insuranceType = FAMILY_INSURED.
     */
    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Policyholder policyholder;

    /**
     * Legal guardian information for MINOR patients (under 18 years old).
     * This is the Erziehungsberechtigter who has legal custody and is authorized
     * to make medical decisions for the minor patient.
     * Required if patient isMinor() = true.
     * May be the same person as the policyholder, but tracked separately.
     */
    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private LegalGuardian guardian;

    /**
     * Form submissions by this patient
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormSubmission> formSubmissions = new ArrayList<>();

    /**
     * Consents given by this patient
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consent> consents = new ArrayList<>();

    /**
     * Signatures by this patient
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Signature> signatures = new ArrayList<>();

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Calculate data retention date (10 years from last visit/submission)
     */
    public void calculateRetentionDate() {
        this.dataRetentionUntil = LocalDate.now().plusYears(10);
    }

    /**
     * Check if patient data should be deleted based on retention policy
     */
    public boolean shouldBeDeleted() {
        if (deletionRequested) {
            return true;
        }
        if (dataRetentionUntil != null) {
            return LocalDate.now().isAfter(dataRetentionUntil);
        }
        return false;
    }

    /**
     * Mark patient for deletion (GDPR Right to be forgotten)
     */
    public void requestDeletion() {
        this.deletionRequested = true;
        this.deletionRequestedDate = LocalDateTime.now();
    }

    /**
     * Grant data processing consent
     */
    public void grantConsent() {
        this.dataProcessingConsent = true;
        this.dataProcessingConsentDate = LocalDateTime.now();
    }

    /**
     * Revoke data processing consent
     */
    public void revokeConsent() {
        this.dataProcessingConsent = false;
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ===================================
    // Insurance Type Helper Methods
    // ===================================

    /**
     * Check if patient is self-insured (has own insurance policy)
     */
    public boolean isSelfInsured() {
        return insuranceType == InsuranceType.SELF_INSURED;
    }

    /**
     * Check if patient is family-insured (covered under guardian's policy)
     */
    public boolean isFamilyInsured() {
        return insuranceType == InsuranceType.FAMILY_INSURED;
    }

    /**
     * Check if patient is a minor (under 18 years old).
     * Uses accurate age calculation with Period.between() to account for
     * full birth dates, not just year difference.
     *
     * German legal age of majority (Volljährigkeit): 18 years
     */
    public boolean isMinor() {
        if (birthDate == null) {
            return false;
        }
        return Period.between(birthDate, LocalDate.now()).getYears() < 18;
    }

    /**
     * Get patient's current age in years
     */
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Validate insurance relationship consistency.
     * If FAMILY_INSURED, must have a policyholder (Hauptversicherter).
     * @throws IllegalStateException if validation fails
     */
    public void validateInsuranceRelationship() {
        if (insuranceType == InsuranceType.FAMILY_INSURED && policyholder == null) {
            throw new IllegalStateException(
                "FAMILY_INSURED patients must have a policyholder (Hauptversicherter)"
            );
        }
    }

    /**
     * Validate guardian relationship for minor patients.
     * Business Rule: Minor patients (under 18) MUST have a legal guardian.
     * Business Rule: Minor patients MUST be FAMILY_INSURED (no self-insured minors).
     *
     * @throws IllegalStateException if validation fails
     */
    public void validateGuardianRelationship() {
        if (isMinor()) {
            // Rule 1: All minors must have a legal guardian
            if (guardian == null) {
                throw new IllegalStateException(
                    "Minor patients (under 18 years) must have a legal guardian (Erziehungsberechtigter)"
                );
            }

            // Rule 2: All minors must be family-insured (no self-insured minors)
            if (insuranceType != InsuranceType.FAMILY_INSURED) {
                throw new IllegalStateException(
                    "Minor patients must be FAMILY_INSURED (Familienversichert). " +
                    "Self-insured minors are not allowed."
                );
            }

            // Rule 3: Family-insured minors must have a policyholder
            if (policyholder == null) {
                throw new IllegalStateException(
                    "Minor family-insured patients must have both a guardian and a policyholder"
                );
            }
        }
    }

    /**
     * Validate all business rules for patient.
     * Call this before saving to ensure data consistency.
     *
     * @throws IllegalStateException if any validation fails
     */
    public void validate() {
        validateInsuranceRelationship();
        validateGuardianRelationship();
    }

    /**
     * Get insurance type display name in German
     */
    public String getInsuranceTypeGerman() {
        return insuranceType != null ? insuranceType.getGermanName() : "";
    }

    /**
     * Get insurance type display name in English
     */
    public String getInsuranceTypeEnglish() {
        return insuranceType != null ? insuranceType.getEnglishName() : "";
    }
}
