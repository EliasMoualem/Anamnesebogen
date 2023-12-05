package de.elias.moualem.Aufnahmebogen.model;

import jakarta.validation.constraints.NotEmpty;

import jakarta.validation.constraints.NotNull;
import lombok.*;


@Data
@RequiredArgsConstructor
public class MinorPatient {

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
    private String signatureUri;
    private MainInsuranceParent mainInsuranceParent;
}