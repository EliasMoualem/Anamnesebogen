package de.elias.moualem.Anamnesebogen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for legal guardian (Erziehungsberechtigter) information.
 *
 * Represents the legal representative of a minor patient (under 18 years old).
 * The guardian has legal custody and is authorized to make medical decisions.
 *
 * This is separate from the insurance policyholder - they may be the same person or different.
 *
 * German terminology:
 * - Erziehungsberechtigter: Legal guardian/authorized representative
 * - Sorgerecht: Legal custody
 * - Volljährigkeit: Age of majority (18 years in Germany)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianDTO {

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
     * Relationship of the guardian to the minor patient.
     * E.g., MOTHER, FATHER, LEGAL_GUARDIAN, GRANDPARENT, OTHER
     */
    @NotNull
    private String relationshipType;

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
