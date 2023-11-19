package de.elias.moualem.Aufnahmebogen.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Patient {

    private String lastName;
    private String firstName;
    private String birthDate;
    private String gender;
    private String street;
    private String zipCode;
    private String city;
    private String mobileNumber;
    private String phoneNumber;
    private String emailAddress;
}