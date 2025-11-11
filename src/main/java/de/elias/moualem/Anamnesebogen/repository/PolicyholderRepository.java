package de.elias.moualem.Anamnesebogen.repository;

import de.elias.moualem.Anamnesebogen.entity.Policyholder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Policyholder entity.
 * Manages Hauptversicherte (main insurance policyholders) for FAMILY_INSURED patients.
 */
@Repository
public interface PolicyholderRepository extends JpaRepository<Policyholder, UUID> {

    /**
     * Find policyholder by patient ID
     */
    @Query("SELECT p FROM Policyholder p WHERE p.patient.id = :patientId")
    Optional<Policyholder> findByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find policyholder by email address
     */
    Optional<Policyholder> findByEmailAddress(String emailAddress);

    /**
     * Check if a patient has a policyholder (Hauptversicherter)
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Policyholder p " +
           "WHERE p.patient.id = :patientId")
    boolean existsByPatientId(@Param("patientId") UUID patientId);
}
