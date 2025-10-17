package de.elias.moualem.Anamnesebogen.model;

import jakarta.validation.constraints.NotEmpty;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;


@Data
@RequiredArgsConstructor
public class MinorPatient implements Serializable {

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
    private String language;
    private MainInsuranceParent mainInsuranceParent;

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

    public boolean hasSignature() {
        return signature != null && signature.length > 0;
    }
}