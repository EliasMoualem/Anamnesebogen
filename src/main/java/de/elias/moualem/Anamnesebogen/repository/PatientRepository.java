package de.elias.moualem.Anamnesebogen.repository;

import de.elias.moualem.Anamnesebogen.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Patient entity.
 * Provides GDPR-compliant data access methods.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Find patient by email address (excluding soft-deleted).
     * Returns the most recently created patient if multiple matches exist.
     */
    @Query("SELECT p FROM Patient p WHERE p.emailAddress = :email AND p.deletedAt IS NULL " +
           "ORDER BY p.createdAt DESC")
    List<Patient> findByEmailAddress(@Param("email") String email);

    /**
     * Find all non-deleted patients
     */
    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL")
    List<Patient> findAllActive();

    /**
     * Find patients whose data should be deleted based on retention policy
     */
    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "(p.deletionRequested = true OR p.dataRetentionUntil < :currentDate)")
    List<Patient> findPatientsForDeletion(@Param("currentDate") LocalDate currentDate);

    /**
     * Find patients who have not consented to data processing
     */
    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND p.dataProcessingConsent = false")
    List<Patient> findPatientsWithoutConsent();

    /**
     * Find patient by first name, last name, and birth date (for duplicate detection).
     * Returns the most recently created patient if multiple matches exist.
     */
    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "LOWER(p.firstName) = LOWER(:firstName) AND " +
           "LOWER(p.lastName) = LOWER(:lastName) AND " +
           "p.birthDate = :birthDate " +
           "ORDER BY p.createdAt DESC")
    List<Patient> findByFirstNameAndLastNameAndBirthDate(
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("birthDate") LocalDate birthDate
    );

    /**
     * Find patients by name (for search functionality)
     */
    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Patient> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Count all active (non-deleted) patients
     */
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.deletedAt IS NULL")
    long countActive();

    /**
     * Find patients created between dates (for reporting)
     */
    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "p.createdAt BETWEEN :startDate AND :endDate")
    List<Patient> findByCreatedAtBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
}
