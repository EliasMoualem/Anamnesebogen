package de.elias.moualem.Anamnesebogen.entity;

/**
 * Insurance type for German health insurance system (Krankenversicherung).
 *
 * German terminology:
 * - SELF_INSURED: Selbstversichert (patient has their own insurance policy)
 * - FAMILY_INSURED: Familienversichert (patient is covered under parent/guardian's policy)
 */
public enum InsuranceType {

    /**
     * SELF_INSURED (Selbstversichert)
     * Patient has their own health insurance policy.
     * Typically for adults (≥18 years) or employed minors.
     */
    SELF_INSURED,

    /**
     * FAMILY_INSURED (Familienversichert)
     * Patient is covered under a family member's insurance policy.
     * The family member is the Hauptversicherter (main policyholder).
     * Typically for minors (<18 years) covered under parent's insurance.
     */
    FAMILY_INSURED;

    /**
     * Get display name in German
     */
    public String getGermanName() {
        return switch (this) {
            case SELF_INSURED -> "Selbstversichert";
            case FAMILY_INSURED -> "Familienversichert";
        };
    }

    /**
     * Get display name in English
     */
    public String getEnglishName() {
        return switch (this) {
            case SELF_INSURED -> "Self-insured";
            case FAMILY_INSURED -> "Family-insured";
        };
    }
}
