package de.elias.moualem.Anamnesebogen.repository;

import de.elias.moualem.Anamnesebogen.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Signature entity.
 */
@Repository
public interface SignatureRepository extends JpaRepository<Signature, UUID> {

    /**
     * Find all signatures for a specific patient
     */
    @Query("SELECT s FROM Signature s WHERE s.patient.id = :patientId ORDER BY s.signedAt DESC")
    List<Signature> findByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find the latest signature for a specific patient
     */
    @Query("SELECT s FROM Signature s WHERE s.patient.id = :patientId ORDER BY s.signedAt DESC LIMIT 1")
    java.util.Optional<Signature> findLatestByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find signatures by document type
     */
    List<Signature> findByDocumentType(Signature.DocumentType documentType);

    /**
     * Find signatures by form submission
     */
    @Query("SELECT s FROM Signature s WHERE s.formSubmission.id = :submissionId")
    List<Signature> findByFormSubmissionId(@Param("submissionId") UUID submissionId);

    /**
     * Find signatures that may be tampered
     */
    @Query("SELECT s FROM Signature s WHERE s.tampered = true OR s.integrityVerified = false")
    List<Signature> findPotentiallyTamperedSignatures();

    /**
     * Find signatures by signature type (for compliance audits)
     */
    List<Signature> findBySignatureType(Signature.SignatureType signatureType);

    /**
     * Find signatures needing integrity verification
     */
    @Query("SELECT s FROM Signature s WHERE s.lastIntegrityCheck IS NULL OR " +
           "s.lastIntegrityCheck < :checkDate")
    List<Signature> findSignaturesNeedingVerification(@Param("checkDate") LocalDateTime checkDate);

    /**
     * Count signatures by patient
     */
    @Query("SELECT COUNT(s) FROM Signature s WHERE s.patient.id = :patientId")
    long countByPatientId(@Param("patientId") UUID patientId);
}
