package de.elias.moualem.Anamnesebogen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consent entity tracking GDPR-compliant consent records.
 * Maintains audit trail of all consent granted and withdrawn.
 */
@Entity
@Table(name = "consents", indexes = {
    @Index(name = "idx_consent_patient", columnList = "patient_id"),
    @Index(name = "idx_consent_type", columnList = "consent_type"),
    @Index(name = "idx_consent_status", columnList = "status"),
    @Index(name = "idx_consent_date", columnList = "consent_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consent extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ===================================
    // Consent Information
    // ===================================

    /**
     * Type of consent
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    /**
     * Current status of the consent
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ConsentStatus status = ConsentStatus.GRANTED;

    /**
     * When consent was granted
     */
    @Column(name = "consent_date", nullable = false)
    @Builder.Default
    private LocalDateTime consentDate = LocalDateTime.now();

    /**
     * When consent was withdrawn (if applicable)
     */
    @Column(name = "withdrawal_date")
    private LocalDateTime withdrawalDate;

    /**
     * Version of the consent text that was agreed to
     */
    @Column(name = "consent_version", nullable = false, length = 20)
    private String consentVersion;

    /**
     * The actual consent text that was presented
     */
    @Column(name = "consent_text", columnDefinition = "TEXT", nullable = false)
    private String consentText;

    /**
     * Language of the consent
     */
    @Column(name = "language", length = 10)
    private String language;

    // ===================================
    // Capture Method
    // ===================================

    /**
     * How was the consent captured
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "capture_method", nullable = false, length = 30)
    private CaptureMethod captureMethod;

    /**
     * IP address from which consent was given
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Device information
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    // ===================================
    // Proof of Consent
    // ===================================

    /**
     * Proof type (checkbox, signature, verbal, written)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 30)
    private ProofType proofType;

    /**
     * Reference to signature if proof was via signature
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signature_id")
    private Signature signature;

    /**
     * Any additional proof data (e.g., screenshot, recording reference)
     */
    @Column(name = "proof_data", columnDefinition = "TEXT")
    private String proofData;

    // ===================================
    // Validity Period
    // ===================================

    /**
     * When this consent expires (if applicable)
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Whether the consent needs periodic renewal
     */
    @Column(name = "requires_renewal")
    @Builder.Default
    private Boolean requiresRenewal = false;

    /**
     * Renewal period in months
     */
    @Column(name = "renewal_period_months")
    private Integer renewalPeriodMonths;

    // ===================================
    // Withdrawal Information
    // ===================================

    /**
     * Reason for withdrawal (if withdrawn)
     */
    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    private String withdrawalReason;

    /**
     * Method used to withdraw consent
     */
    @Column(name = "withdrawal_method", length = 30)
    private String withdrawalMethod;

    // ===================================
    // Legal and Compliance
    // ===================================

    /**
     * Legal basis for processing (GDPR Article 6)
     */
    @Column(name = "legal_basis", length = 100)
    private String legalBasis;

    /**
     * Purpose of data processing
     */
    @Column(name = "processing_purpose", columnDefinition = "TEXT")
    private String processingPurpose;

    /**
     * Data categories covered by this consent
     */
    @Column(name = "data_categories", columnDefinition = "TEXT")
    private String dataCategories;

    // ===================================
    // Relationships
    // ===================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Withdraw this consent
     */
    public void withdraw(String reason, String method) {
        this.status = ConsentStatus.WITHDRAWN;
        this.withdrawalDate = LocalDateTime.now();
        this.withdrawalReason = reason;
        this.withdrawalMethod = method;
    }

    /**
     * Check if consent is currently valid
     */
    public boolean isValid() {
        if (status != ConsentStatus.GRANTED) {
            return false;
        }
        if (expiryDate != null && LocalDateTime.now().isAfter(expiryDate)) {
            return false;
        }
        return true;
    }

    /**
     * Check if consent needs renewal
     */
    public boolean needsRenewal() {
        if (!requiresRenewal || renewalPeriodMonths == null) {
            return false;
        }
        LocalDateTime renewalDue = consentDate.plusMonths(renewalPeriodMonths);
        return LocalDateTime.now().isAfter(renewalDue);
    }

    // ===================================
    // Enums
    // ===================================

    public enum ConsentType {
        /**
         * GDPR Article 6(1)(a) - Data processing consent
         */
        DATA_PROCESSING,

        /**
         * Marketing communications
         */
        MARKETING,

        /**
         * Third-party data sharing
         */
        DATA_SHARING,

        /**
         * Medical treatment consent
         */
        MEDICAL_TREATMENT,

        /**
         * Billing and insurance
         */
        BILLING,

        /**
         * Photography/video recording
         */
        MEDIA,

        /**
         * Research participation
         */
        RESEARCH,

        /**
         * Telemedicine/digital health
         */
        TELEMEDICINE,

        /**
         * Emergency contact
         */
        EMERGENCY_CONTACT,

        /**
         * Other consent types
         */
        OTHER
    }

    public enum ConsentStatus {
        GRANTED,        // Consent is active
        WITHDRAWN,      // Consent has been withdrawn
        EXPIRED,        // Consent has expired
        PENDING         // Awaiting consent decision
    }

    public enum CaptureMethod {
        WEB_FORM,       // Captured via web form
        MOBILE_APP,     // Captured via mobile app
        EMAIL,          // Email confirmation
        IN_PERSON,      // In-person at practice
        PHONE,          // Phone call
        PAPER,          // Paper form (later digitized)
        OTHER
    }

    public enum ProofType {
        CHECKBOX,           // Simple checkbox agreement
        ELECTRONIC_SIGNATURE, // Electronic signature
        HANDWRITTEN_SIGNATURE, // Scanned handwritten signature
        VERBAL,             // Verbal consent (recorded)
        TWO_FACTOR,         // Two-factor confirmation
        BIOMETRIC,          // Biometric verification
        OTHER
    }
}
