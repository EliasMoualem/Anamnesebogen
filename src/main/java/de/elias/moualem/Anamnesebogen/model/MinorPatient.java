package de.elias.moualem.Anamnesebogen.model;

import jakarta.validation.constraints.NotEmpty;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;


@Data
@RequiredArgsConstructor
public class MinorPatient implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public boolean hasSignature() {
        return signature != null && signature.length > 0;
    }
}