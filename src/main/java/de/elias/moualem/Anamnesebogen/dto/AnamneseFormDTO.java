package de.elias.moualem.Anamnesebogen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for anamnesis form submission.
 * Used to transfer data from web forms to the service layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnamneseFormDTO {

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

    @Email
    private String emailAddress;

    private String language;

    // Policyholder Information (for FAMILY_INSURED patients)
    private PolicyholderDTO policyholder;

    // Insurance Information
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

    // Signature Data (base64 encoded)
    private String signatureData;

    // Consent
    private Boolean dataProcessingConsent;

    // Submission metadata
    private String submissionIpAddress;
    private String submissionUserAgent;
}
