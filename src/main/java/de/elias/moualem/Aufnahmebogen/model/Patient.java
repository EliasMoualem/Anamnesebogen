package de.elias.moualem.Aufnahmebogen.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class Patient {

    private String lastName;
    private String firstName;
    private Date birthDate;
    private String gender;
}