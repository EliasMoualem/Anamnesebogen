package de.elias.moualem.Anamnesebogen.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuditLog entity tracking all data access and operations for GDPR Article 30 compliance.
 * Records who accessed what data, when, why, and from where.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_patient", columnList = "patient_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ===================================
    // When & Who
    // ===================================

    /**
     * When the action occurred
     */
    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * User ID who performed the action (null for patient actions)
     */
    @Column(name = "user_id")
    private UUID userId;

    /**
     * Username or email of the user
     */
    @Column(name = "username", length = 255)
    private String username;

    /**
     * Role of the user (ADMIN, DOCTOR, STAFF, PATIENT)
     */
    @Column(name = "user_role", length = 50)
    private String userRole;

    /**
     * Patient ID if the action involves a specific patient
     */
    @Column(name = "patient_id")
    private UUID patientId;

    // ===================================
    // What Action
    // ===================================

    /**
     * Type of action performed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    /**
     * Entity type that was accessed/modified
     */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /**
     * ID of the entity that was accessed/modified
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Detailed description of the action
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ===================================
    // Where & How
    // ===================================

    /**
     * IP address from which the action was performed
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
     * Request method (GET, POST, PUT, DELETE)
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /**
     * Request URI/endpoint
     */
    @Column(name = "request_uri", length = 500)
    private String requestUri;

    /**
     * Session ID
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    // ===================================
    // What Changed
    // ===================================

    /**
     * Old values before change (JSON format)
     */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    /**
     * New values after change (JSON format)
     */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    /**
     * Fields that were modified (comma-separated)
     */
    @Column(name = "modified_fields", length = 500)
    private String modifiedFields;

    // ===================================
    // Result & Status
    // ===================================

    /**
     * Whether the action was successful
     */
    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;

    /**
     * Error message if action failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * HTTP status code
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * Duration of the operation in milliseconds
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    // ===================================
    // GDPR & Compliance
    // ===================================

    /**
     * Legal basis for the data access/processing
     */
    @Column(name = "legal_basis", length = 100)
    private String legalBasis;

    /**
     * Purpose of the data access
     */
    @Column(name = "access_purpose", length = 255)
    private String accessPurpose;

    /**
     * Data categories accessed
     */
    @Column(name = "data_categories", length = 500)
    private String dataCategories;

    /**
     * Whether patient was notified of this access
     */
    @Column(name = "patient_notified")
    @Builder.Default
    private Boolean patientNotified = false;

    // ===================================
    // Security & Alerts
    // ===================================

    /**
     * Security level of the action
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "security_level", length = 20)
    @Builder.Default
    private SecurityLevel securityLevel = SecurityLevel.NORMAL;

    /**
     * Whether this action triggered a security alert
     */
    @Column(name = "security_alert")
    @Builder.Default
    private Boolean securityAlert = false;

    /**
     * Alert reason if security alert was triggered
     */
    @Column(name = "alert_reason", length = 500)
    private String alertReason;

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Mark action as failed
     */
    public void markFailed(String errorMessage, Integer statusCode) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    /**
     * Trigger security alert
     */
    public void triggerSecurityAlert(String reason) {
        this.securityAlert = true;
        this.alertReason = reason;
        this.securityLevel = SecurityLevel.HIGH;
    }

    // ===================================
    // Enums
    // ===================================

    public enum ActionType {
        // Read operations
        VIEW,
        EXPORT,
        DOWNLOAD,
        SEARCH,
        LIST,

        // Write operations
        CREATE,
        UPDATE,
        DELETE,
        SOFT_DELETE,
        RESTORE,

        // Authentication
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        PASSWORD_RESET,
        MFA_SETUP,

        // Consent operations
        CONSENT_GRANTED,
        CONSENT_WITHDRAWN,
        CONSENT_UPDATED,

        // GDPR rights
        DATA_ACCESS_REQUEST,
        DATA_EXPORT_REQUEST,
        DATA_DELETION_REQUEST,
        DATA_RECTIFICATION_REQUEST,
        DATA_PORTABILITY_REQUEST,

        // Signature operations
        SIGNATURE_CAPTURED,
        SIGNATURE_VERIFIED,

        // Form operations
        FORM_SUBMITTED,
        FORM_VIEWED,
        PDF_GENERATED,

        // System operations
        BACKUP_CREATED,
        BACKUP_RESTORED,
        CONFIG_CHANGED,
        USER_CREATED,
        USER_DELETED,
        ROLE_CHANGED,

        // Security events
        UNAUTHORIZED_ACCESS,
        SUSPICIOUS_ACTIVITY,
        DATA_BREACH_DETECTED,

        // Other
        OTHER
    }

    public enum SecurityLevel {
        LOW,        // Normal operations
        NORMAL,     // Standard operations
        HIGH,       // Sensitive data access
        CRITICAL    // Security incidents
    }
}
