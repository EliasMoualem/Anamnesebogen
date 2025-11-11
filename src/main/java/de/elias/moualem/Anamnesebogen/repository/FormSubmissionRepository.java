package de.elias.moualem.Anamnesebogen.repository;

import de.elias.moualem.Anamnesebogen.entity.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for FormSubmission entity.
 */
@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {

    /**
     * Find all submissions for a specific patient
     */
    @Query("SELECT fs FROM FormSubmission fs WHERE fs.patient.id = :patientId ORDER BY fs.submissionDate DESC")
    List<FormSubmission> findByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find submissions by status
     */
    List<FormSubmission> findByStatus(FormSubmission.SubmissionStatus status);

    /**
     * Find submissions within date range
     */
    @Query("SELECT fs FROM FormSubmission fs WHERE fs.submissionDate BETWEEN :startDate AND :endDate")
    List<FormSubmission> findBySubmissionDateBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find submissions with failed status for retry
     */
    @Query("SELECT fs FROM FormSubmission fs WHERE fs.status = 'FAILED' ORDER BY fs.submissionDate DESC")
    List<FormSubmission> findFailedSubmissions();

    /**
     * Count submissions by patient
     */
    @Query("SELECT COUNT(fs) FROM FormSubmission fs WHERE fs.patient.id = :patientId")
    long countByPatientId(@Param("patientId") UUID patientId);
}
