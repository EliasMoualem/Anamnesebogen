package de.elias.moualem.Anamnesebogen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FormSubmission entity tracking when and how patients submitted anamnesis forms.
 * Maintains audit trail for compliance purposes.
 */
@Entity
@Table(name = "form_submissions", indexes = {
    @Index(name = "idx_submission_date", columnList = "submission_date"),
    @Index(name = "idx_submission_patient", columnList = "patient_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSubmission extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ===================================
    // Submission Metadata
    // ===================================

    @Column(name = "submission_date", nullable = false)
    @Builder.Default
    private LocalDateTime submissionDate = LocalDateTime.now();

    @Column(name = "form_language", length = 10)
    private String formLanguage;

    @Column(name = "form_version", length = 20)
    private String formVersion;

    /**
     * IP address from which the form was submitted
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from submission
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Device information (mobile, tablet, desktop)
     */
    @Column(name = "device_type", length = 50)
    private String deviceType;

    // ===================================
    // PDF Generation
    // ===================================

    /**
     * Path to generated PDF file (if stored in S3/file system)
     */
    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath;

    /**
     * When the PDF was generated
     */
    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    /**
     * Hash of the PDF content for tamper detection
     */
    @Column(name = "pdf_hash", length = 64)
    private String pdfHash;

    // ===================================
    // Status Tracking
    // ===================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    /**
     * Additional notes or error messages
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ===================================
    // Dynamic Form Builder Fields
    // ===================================

    /**
     * Reference to the form definition used for this submission.
     * Tracks which version of the form was used.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id")
    private FormDefinition formDefinition;

    /**
     * Complete snapshot of form data at submission time (for historical record).
     * Stored as JSONB to preserve exact data even if form definition changes.
     *
     * Structure:
     * {
     *   "formVersion": "1.0.0",
     *   "submittedFields": {
     *     "firstName": "Max",
     *     "lastName": "Mustermann",
     *     "hasAllergies": true,
     *     "allergyDetails": "Pollen"
     *   },
     *   "metadata": {
     *     "formId": "uuid",
     *     "language": "de",
     *     "completionTime": "00:05:23"
     *   }
     * }
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "form_data_snapshot", columnDefinition = "jsonb")
    private com.fasterxml.jackson.databind.JsonNode formDataSnapshot;

    // ===================================
    // Relationships
    // ===================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Signatures associated with this submission
     */
    @OneToMany(mappedBy = "formSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Signature> signatures = new ArrayList<>();

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Mark submission as completed with PDF generated
     */
    public void markCompleted(String pdfPath, String hash) {
        this.status = SubmissionStatus.COMPLETED;
        this.pdfFilePath = pdfPath;
        this.pdfHash = hash;
        this.pdfGeneratedAt = LocalDateTime.now();
    }

    /**
     * Mark submission as failed
     */
    public void markFailed(String errorMessage) {
        this.status = SubmissionStatus.FAILED;
        this.notes = errorMessage;
    }

    /**
     * Add a signature to this submission
     */
    public void addSignature(Signature signature) {
        signatures.add(signature);
        signature.setFormSubmission(this);
    }

    // ===================================
    // Enums
    // ===================================

    public enum SubmissionStatus {
        SUBMITTED,      // Form submitted, processing
        COMPLETED,      // PDF generated successfully
        FAILED,         // Processing failed
        ARCHIVED        // Archived after retention period
    }
}
