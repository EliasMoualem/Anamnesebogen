package de.elias.moualem.Anamnesebogen.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for patient anamnesis form data binding.
 *
 * This DTO is used for binding form data from all patients (both minor and adult).
 * The name "PatientFormDTO" reflects its purpose: binding web form input.
 *
 */
@Data
@RequiredArgsConstructor
public class PatientFormDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // Personal Information
    @NotNull
    private String lastName;
    @NotEmpty
    private String firstName;
    @NotNull
    private String birthDate;
    private String gender;
    private String street;
    private String zipCode;
    private String city;
    private String mobileNumber;
    private String phoneNumber;
    private String emailAddress;
    private byte[] signature;
    private UUID signatureId;
    private LocalDateTime signatureTimestamp;
    private String language;

    // Guardian/Policyholder information
    private GuardianDTO guardian; // Legal guardian for minor patients (under 18)
    private PolicyholderDTO policyholder; // Policyholder for family-insured patients

    // Insurance Information
    private String insuranceType; // SELF_INSURED or FAMILY_INSURED (auto-set by frontend based on age)
    private String insuranceProvider;
    private String insurancePolicyNumber;
    private String insuranceGroupNumber;
    private String policyholderName;
    private String relationshipToPolicyholder;

    // Medical History
    private String allergies;
    private String currentMedications;
    private String medicalConditions;
    private String previousSurgeries;
    private String primaryCareDoctor;

    public boolean hasSignature() {
        return signature != null && signature.length > 0;
    }
}
