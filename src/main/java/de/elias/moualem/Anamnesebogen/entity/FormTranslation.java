package de.elias.moualem.Anamnesebogen.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing translations for a form definition.
 * Each form can have multiple translations (de, en, ar, ru).
 * Translations stored as JSONB for flexibility.
 */
@Entity
@Table(name = "form_translations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"form_definition_id", "language"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the form definition this translation belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_definition_id", nullable = false)
    private FormDefinition formDefinition;

    /**
     * Language code: de, en, ar, ru
     */
    @Column(nullable = false, length = 10)
    private String language;

    /**
     * JSONB object containing all translatable strings for the form.
     *
     * Structure:
     * {
     *   "pages": {
     *     "personalInfo": "Persönliche Daten",
     *     "medicalInfo": "Medizinische Angaben"
     *   },
     *   "fields": {
     *     "firstName": "Vorname",
     *     "lastName": "Nachname",
     *     "hasAllergies": "Haben Sie Allergien?"
     *   },
     *   "placeholders": {
     *     "enterFirstName": "Vorname eingeben"
     *   },
     *   "options": {
     *     "yes": "Ja",
     *     "no": "Nein",
     *     "male": "Männlich",
     *     "female": "Weiblich"
     *   },
     *   "buttons": {
     *     "next": "Weiter",
     *     "previous": "Zurück",
     *     "submit": "Absenden"
     *   },
     *   "validation": {
     *     "required": "Dieses Feld ist erforderlich",
     *     "minLength": "Mindestens {min} Zeichen erforderlich",
     *     "maxLength": "Maximal {max} Zeichen erlaubt",
     *     "email": "Bitte geben Sie eine gültige E-Mail-Adresse ein"
     *   },
     *   "messages": {
     *     "success": "Formular erfolgreich übermittelt",
     *     "error": "Ein Fehler ist aufgetreten"
     *   }
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode translations;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    // ========================================================================
    // Supported Languages
    // ========================================================================

    public enum Language {
        DE("de", "Deutsch", false),
        EN("en", "English", false),
        AR("ar", "العربية", true),  // Arabic - RTL
        RU("ru", "Русский", false);

        private final String code;
        private final String displayName;
        private final boolean isRtl;

        Language(String code, String displayName, boolean isRtl) {
            this.code = code;
            this.displayName = displayName;
            this.isRtl = isRtl;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isRtl() {
            return isRtl;
        }

        public static Language fromCode(String code) {
            for (Language lang : values()) {
                if (lang.code.equals(code)) {
                    return lang;
                }
            }
            throw new IllegalArgumentException("Unknown language code: " + code);
        }

        public static boolean isSupported(String code) {
            try {
                fromCode(code);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    // ========================================================================
    // Business Methods
    // ========================================================================

    /**
     * Check if this is an RTL (right-to-left) language.
     */
    public boolean isRtl() {
        return Language.isSupported(language) && Language.fromCode(language).isRtl();
    }

    /**
     * Get the display name for this language.
     */
    public String getLanguageDisplayName() {
        return Language.isSupported(language) ?
               Language.fromCode(language).getDisplayName() :
               language;
    }

    /**
     * Validate translation and update timestamp before persisting.
     */
    @PrePersist
    @PreUpdate
    public void prePersistAndUpdate() {
        // Validate required fields
        if (formDefinition == null) {
            throw new IllegalStateException("FormDefinition cannot be null");
        }
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalStateException("Language cannot be null or empty");
        }
        if (!Language.isSupported(language)) {
            throw new IllegalStateException("Unsupported language: " + language);
        }
        if (translations == null) {
            throw new IllegalStateException("Translations cannot be null");
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
        if (!(o instanceof FormTranslation)) return false;
        FormTranslation that = (FormTranslation) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FormTranslation{" +
                "id=" + id +
                ", language='" + language + '\'' +
                ", formDefinitionId=" + (formDefinition != null ? formDefinition.getId() : null) +
                '}';
    }
}