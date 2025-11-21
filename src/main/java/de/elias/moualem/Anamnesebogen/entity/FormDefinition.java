package de.elias.moualem.Anamnesebogen.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a dynamic form definition.
 * Stores form structure, validation rules, and UI configuration as JSONB.
 * Supports multi-language translations and versioning.
 */
@Entity
@Table(name = "form_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FormCategory category;

    @Column(nullable = false, length = 20)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FormStatus status = FormStatus.DRAFT;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * JSON Schema defining validation rules for form fields.
     * Example: {"type": "object", "properties": {"firstName": {"type": "string", "minLength": 2}}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode schema;

    /**
     * UI Schema defining form structure, fields, pages, and layout.
     * Example: {"pages": [{"title": "Personal Info", "elements": [...]}]}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_schema", columnDefinition = "jsonb", nullable = false)
    private JsonNode uiSchema;

    /**
     * Additional validation rules beyond JSON Schema.
     * Example: {"customValidations": [{"field": "email", "validator": "email"}]}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private JsonNode validationRules;

    /**
     * UI rendering options (theme, layout preferences, etc.).
     * Example: {"theme": "default", "showProgressBar": true}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rendering_options", columnDefinition = "jsonb")
    private JsonNode renderingOptions;

    /**
     * Field mappings from schema field names to canonical field types.
     * Example: {"vorname": "FIRST_NAME", "nachname": "LAST_NAME", "geburtsdatum": "BIRTH_DATE"}
     * Used to map dynamic form fields to Patient entity properties during submission processing.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mappings", columnDefinition = "jsonb")
    @Builder.Default
    private JsonNode fieldMappings = null;

    /**
     * Translations for this form in different languages.
     * Cascade: Delete translations when form is deleted.
     */
    @OneToMany(mappedBy = "formDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FormTranslation> translations = new ArrayList<>();

    /**
     * Form submissions that used this form definition.
     */
    @OneToMany(mappedBy = "formDefinition", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FormSubmission> submissions = new ArrayList<>();

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private String publishedBy;

    /**
     * Form status lifecycle
     */
    public enum FormStatus {
        DRAFT,      // Form is being edited
        PUBLISHED,  // Form is live and accepting submissions
        ARCHIVED    // Form is no longer active but kept for historical record
    }

    /**
     * Form categories
     */
    public enum FormCategory {
        ANAMNESIS,   // Medical history form
        CONSENT,     // Consent forms (GDPR, treatment consent, etc.)
        TREATMENT,   // Treatment plan forms
        CUSTOM       // Custom forms defined by practice
    }

    // ========================================================================
    // Business Methods
    // ========================================================================

    /**
     * Publish this form and make it active.
     * Only one form per category can be active at a time.
     */
    public void publish(String publishedBy) {
        this.status = FormStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.publishedBy = publishedBy;
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Archive this form (no longer active but kept for record).
     */
    public void archive() {
        this.status = FormStatus.ARCHIVED;
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate this form (make it inactive without archiving).
     */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Add a translation for this form.
     */
    public void addTranslation(FormTranslation translation) {
        translations.add(translation);
        translation.setFormDefinition(this);
    }

    /**
     * Remove a translation from this form.
     */
    public void removeTranslation(FormTranslation translation) {
        translations.remove(translation);
        translation.setFormDefinition(null);
    }

    /**
     * Check if this form is published and active.
     */
    public boolean isPublishedAndActive() {
        return status == FormStatus.PUBLISHED && isActive;
    }

    /**
     * Validate form and update timestamp before persisting.
     */
    @PrePersist
    @PreUpdate
    public void prePersistAndUpdate() {
        // Validate required fields
        if (schema == null) {
            throw new IllegalStateException("Form schema cannot be null");
        }
        if (uiSchema == null) {
            throw new IllegalStateException("Form UI schema cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Form name cannot be null or empty");
        }

        // Update timestamp on every save
        this.updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // Equals and HashCode
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormDefinition)) return false;
        FormDefinition that = (FormDefinition) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FormDefinition{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", version='" + version + '\'' +
                ", status=" + status +
                ", isActive=" + isActive +
                '}';
    }
}