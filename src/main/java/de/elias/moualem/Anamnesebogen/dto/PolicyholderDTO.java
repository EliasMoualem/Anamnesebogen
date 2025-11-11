package de.elias.moualem.Anamnesebogen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for policyholder (Hauptversicherter) information.
 *
 * Represents the main insurance policyholder under whose policy
 * a FAMILY_INSURED patient is covered. Typically a parent or legal guardian.
 *
 * German terminology:
 * - Hauptversicherter: Main policyholder
 * - Mitversicherte Person: Dependent/co-insured person (the patient)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyholderDTO {

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

    @Email
    private String emailAddress;

    private String job;

    /**
     * Helper method to check if all required fields are filled
     */
    public boolean hasData() {
        return firstName != null && !firstName.isEmpty()
                && lastName != null && !lastName.isEmpty();
    }

    /**
     * Helper method to get full name
     */
    public String getFullName() {
        if (firstName == null || lastName == null) {
            return "";
        }
        return firstName + " " + lastName;
    }
}
