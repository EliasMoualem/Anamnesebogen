package de.elias.moualem.Anamnesebogen.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Policyholder entity representing the Hauptversicherter (main insurance policyholder).
 *
 * In the German health insurance system (Krankenversicherung), this represents
 * the person who holds the insurance policy under which a FAMILY_INSURED patient
 * is covered. Typically a parent/legal guardian for minor patients.
 *
 * German terminology:
 * - Hauptversicherter: Main policyholder
 * - Mitversicherte Person: Dependent/co-insured person (the patient)
 * - Familienversicherung: Family insurance coverage
 */
@Entity
@Table(name = "policyholders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policyholder extends BaseAuditEntity {

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
    // Relationships
    // ===================================

    /**
     * The patient (dependent) who is covered under this policyholder's insurance.
     * This is the mitversicherte Person (co-insured dependent).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get role description in German
     */
    public String getRoleGerman() {
        return "Hauptversicherter";
    }

    /**
     * Get role description in English
     */
    public String getRoleEnglish() {
        return "Main Policyholder";
    }
}
