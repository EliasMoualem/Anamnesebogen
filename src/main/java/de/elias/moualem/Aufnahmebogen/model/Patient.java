package de.elias.moualem.Aufnahmebogen.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Patient {

    private String lastName;
    private String firstName;
    private String birthDate;
    private String gender;
}