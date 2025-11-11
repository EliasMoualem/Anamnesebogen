package de.elias.moualem.Anamnesebogen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Signature entity storing digital signatures with audit trail and tamper detection.
 * Complies with German eIDAS requirements for electronic signatures.
 */
@Entity
@Table(name = "signatures", indexes = {
    @Index(name = "idx_signature_date", columnList = "signed_at"),
    @Index(name = "idx_signature_patient", columnList = "patient_id"),
    @Index(name = "idx_signature_document", columnList = "document_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signature extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ===================================
    // Signature Data
    // ===================================

    /**
     * The actual signature image data (PNG format, base64 encoded)
     * In production, this should be encrypted
     */
    @Column(name = "signature_data", nullable = false)
    private byte[] signatureData;

    /**
     * SHA-256 hash of the signature data for tamper detection
     */
    @Column(name = "signature_hash", nullable = false, length = 64)
    private String signatureHash;

    /**
     * MIME type of the signature (usually image/png)
     */
    @Column(name = "signature_mime_type", length = 50)
    @Builder.Default
    private String signatureMimeType = "image/png";

    // ===================================
    // Signature Metadata
    // ===================================

    @Column(name = "signed_at", nullable = false)
    @Builder.Default
    private LocalDateTime signedAt = LocalDateTime.now();

    /**
     * Type of electronic signature according to eIDAS
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 20)
    @Builder.Default
    private SignatureType signatureType = SignatureType.SIMPLE;

    /**
     * Document type being signed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    /**
     * IP address from which signature was captured
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

    /**
     * Geolocation coordinates (if available and consented)
     */
    @Column(name = "geolocation", length = 100)
    private String geolocation;

    // ===================================
    // Legal Compliance Fields
    // ===================================

    /**
     * Full name of the person who signed (for audit purposes)
     */
    @Column(name = "signer_name", nullable = false, length = 200)
    private String signerName;

    /**
     * Intent statement that was agreed to when signing
     */
    @Column(name = "intent_statement", columnDefinition = "TEXT")
    private String intentStatement;

    /**
     * Whether the signer explicitly confirmed their identity
     */
    @Column(name = "identity_confirmed")
    @Builder.Default
    private Boolean identityConfirmed = false;

    /**
     * Method used for identity verification (if any)
     */
    @Column(name = "identity_verification_method", length = 100)
    private String identityVerificationMethod;

    // ===================================
    // Tamper Detection
    // ===================================

    /**
     * Indicates if signature has been verified for tampering
     */
    @Column(name = "integrity_verified")
    @Builder.Default
    private Boolean integrityVerified = false;

    /**
     * Last time integrity was checked
     */
    @Column(name = "last_integrity_check")
    private LocalDateTime lastIntegrityCheck;

    /**
     * If tampering was detected
     */
    @Column(name = "tampered")
    @Builder.Default
    private Boolean tampered = false;

    // ===================================
    // Relationships
    // ===================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_submission_id")
    private FormSubmission formSubmission;

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Verify signature integrity by checking hash
     */
    public boolean verifyIntegrity(String expectedHash) {
        this.lastIntegrityCheck = LocalDateTime.now();
        this.integrityVerified = this.signatureHash.equals(expectedHash);
        this.tampered = !this.integrityVerified;
        return this.integrityVerified;
    }

    /**
     * Check if signature is valid (not tampered and within validity period)
     */
    public boolean isValid() {
        if (tampered) {
            return false;
        }
        // Additional validation logic can be added here
        return true;
    }

    // ===================================
    // Enums
    // ===================================

    /**
     * Electronic signature types according to eIDAS Regulation (EU) No 910/2014
     */
    public enum SignatureType {
        /**
         * Simple Electronic Signature (SES)
         * Basic electronic signature, lowest level of security
         * Suitable for most dental practice documents
         */
        SIMPLE,

        /**
         * Advanced Electronic Signature (AES)
         * Uniquely linked to the signatory, capable of identifying the signatory
         * Created using means under signatory's sole control
         * Linked to data in a manner that detects tampering
         */
        ADVANCED,

        /**
         * Qualified Electronic Signature (QES)
         * Advanced signature created by qualified signature creation device
         * Based on qualified certificate for electronic signatures
         * Equivalent to handwritten signature in EU
         */
        QUALIFIED
    }

    /**
     * Document types requiring signatures in German dental practices
     */
    public enum DocumentType {
        /**
         * Einwilligung zur Weitergabe von Behandlungsdaten (EWE)
         * Patient consent for billing data transmission
         */
        EWE,

        /**
         * Honorarvereinbarung (exceeding factor 3.5)
         * Fee agreement
         */
        HONORARVEREINBARUNG,

        /**
         * Treatment plan for non-covered services
         */
        VERLANGENSLEISTUNGEN,

        /**
         * Additional cost agreement for fillings (MKV Füllung)
         */
        MKV_FUELLUNG,

        /**
         * Orthodontic treatment agreement
         */
        ORTHODONTIC,

        /**
         * General anamnesis form
         */
        ANAMNESIS,

        /**
         * General consent form
         */
        GENERAL_CONSENT,

        /**
         * GDPR data processing consent
         */
        DATA_PROCESSING_CONSENT,

        /**
         * Other document types
         */
        OTHER
    }
}
