package de.elias.moualem.Anamnesebogen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * LegalGuardian entity representing the legal representative (Erziehungsberechtigter) of a minor patient.
 *
 * In the German legal system, this represents the person who has legal custody (Sorgerecht)
 * and is authorized to make medical decisions for a minor patient (under 18 years of age).
 *
 * This entity is separate from Policyholder - a guardian may or may not be the insurance policyholder.
 * For example:
 * - Mother is guardian, Father is policyholder
 * - Parent is guardian, Grandparent is policyholder
 * - Guardian and policyholder are the same person (common case)
 *
 * German terminology:
 * - Erziehungsberechtigter: Legal guardian/authorized representative
 * - Sorgerecht: Legal custody
 * - Volljährigkeit: Age of majority (18 years in Germany)
 */
@Entity
@Table(name = "legal_guardians", indexes = {
    @Index(name = "idx_guardian_patient", columnList = "patient_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalGuardian extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ===================================
    // Personal Information
    // ===================================

    @NotNull
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotEmpty
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "birth_date")
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

    @Column(name = "job", length = 255)
    private String job;

    // ===================================
    // Guardian-Specific Fields
    // ===================================

    /**
     * Relationship of the guardian to the minor patient.
     * E.g., MOTHER, FATHER, LEGAL_GUARDIAN, GRANDPARENT
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", length = 30, nullable = false)
    @NotNull
    private RelationshipType relationshipType;

    // ===================================
    // Relationships
    // ===================================

    /**
     * The minor patient for whom this person is the legal guardian.
     * Required - a guardian must be associated with a patient.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Get full name of guardian
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get relationship description in German
     */
    public String getRelationshipGerman() {
        return switch (relationshipType) {
            case MOTHER -> "Mutter";
            case FATHER -> "Vater";
            case LEGAL_GUARDIAN -> "Erziehungsberechtigter";
            case GRANDPARENT -> "Großelternteil";
            case OTHER -> "Andere";
        };
    }

    /**
     * Get relationship description in English
     */
    public String getRelationshipEnglish() {
        return switch (relationshipType) {
            case MOTHER -> "Mother";
            case FATHER -> "Father";
            case LEGAL_GUARDIAN -> "Legal Guardian";
            case GRANDPARENT -> "Grandparent";
            case OTHER -> "Other";
        };
    }

    /**
     * Get role description in German
     */
    public String getRoleGerman() {
        return "Erziehungsberechtigter";
    }

    /**
     * Get role description in English
     */
    public String getRoleEnglish() {
        return "Legal Guardian";
    }

    // ===================================
    // Enums
    // ===================================

    /**
     * Type of relationship between guardian and minor patient.
     * German: Verwandtschaftsverhältnis
     */
    public enum RelationshipType {
        /**
         * Mother (Mutter)
         */
        MOTHER,

        /**
         * Father (Vater)
         */
        FATHER,

        /**
         * Legal guardian (Vormund)
         * Used when guardian is not a biological parent
         */
        LEGAL_GUARDIAN,

        /**
         * Grandparent (Großelternteil)
         * May have custody in some family situations
         */
        GRANDPARENT,

        /**
         * Other relationship
         * E.g., foster parent, legal custodian
         */
        OTHER
    }
}
