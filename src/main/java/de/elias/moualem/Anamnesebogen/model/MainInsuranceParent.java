package de.elias.moualem.Anamnesebogen.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class MainInsuranceParent {

    @NotNull
    private String lastName;
    @NotEmpty
    private String firstName;
    private String birthDate;
    private String gender;
    private String street;
    private String zipCode;
    private String city;
    private String mobileNumber;
    private String phoneNumber;
    private String emailAddress;
    private String job;
    private String signatureUri;
}
