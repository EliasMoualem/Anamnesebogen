package de.elias.moualem.Anamnesebogen.repository;

import de.elias.moualem.Anamnesebogen.entity.LegalGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LegalGuardian entity.
 * Manages Erziehungsberechtigte (legal guardians) for MINOR patients (under 18 years old).
 */
@Repository
public interface LegalGuardianRepository extends JpaRepository<LegalGuardian, UUID> {

    /**
     * Find legal guardian by patient ID
     */
    @Query("SELECT lg FROM LegalGuardian lg WHERE lg.patient.id = :patientId")
    Optional<LegalGuardian> findByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find legal guardian by email address
     */
    Optional<LegalGuardian> findByEmailAddress(String emailAddress);

    /**
     * Check if a patient has a legal guardian
     */
    @Query("SELECT CASE WHEN COUNT(lg) > 0 THEN true ELSE false END FROM LegalGuardian lg " +
           "WHERE lg.patient.id = :patientId")
    boolean existsByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find all legal guardians by relationship type
     */
    @Query("SELECT lg FROM LegalGuardian lg WHERE lg.relationshipType = :relationshipType")
    Optional<LegalGuardian> findByRelationshipType(@Param("relationshipType") LegalGuardian.RelationshipType relationshipType);
}
