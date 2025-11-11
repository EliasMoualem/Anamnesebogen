package de.elias.moualem.Anamnesebogen.repository;

import de.elias.moualem.Anamnesebogen.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Consent entity.
 * Provides GDPR-compliant consent management.
 */
@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    /**
     * Find all consents for a specific patient
     */
    @Query("SELECT c FROM Consent c WHERE c.patient.id = :patientId ORDER BY c.consentDate DESC")
    List<Consent> findByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find active (granted) consents for a patient
     */
    @Query("SELECT c FROM Consent c WHERE c.patient.id = :patientId AND c.status = 'GRANTED'")
    List<Consent> findActiveConsentsByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find specific consent type for a patient
     */
    @Query("SELECT c FROM Consent c WHERE c.patient.id = :patientId AND c.consentType = :consentType " +
           "ORDER BY c.consentDate DESC")
    List<Consent> findByPatientIdAndConsentType(
        @Param("patientId") UUID patientId,
        @Param("consentType") Consent.ConsentType consentType
    );

    /**
     * Find the most recent active consent of a specific type for a patient
     */
    @Query("SELECT c FROM Consent c WHERE c.patient.id = :patientId AND c.consentType = :consentType " +
           "AND c.status = 'GRANTED' ORDER BY c.consentDate DESC")
    Optional<Consent> findLatestActiveConsentByType(
        @Param("patientId") UUID patientId,
        @Param("consentType") Consent.ConsentType consentType
    );

    /**
     * Find consents by status
     */
    List<Consent> findByStatus(Consent.ConsentStatus status);

    /**
     * Find consents that need renewal
     */
    @Query("SELECT c FROM Consent c WHERE c.status = 'GRANTED' AND c.requiresRenewal = true AND " +
           "c.consentDate < :renewalDate")
    List<Consent> findConsentsNeedingRenewal(@Param("renewalDate") LocalDateTime renewalDate);

    /**
     * Find expired consents
     */
    @Query("SELECT c FROM Consent c WHERE c.status = 'GRANTED' AND c.expiryDate IS NOT NULL AND " +
           "c.expiryDate < :currentDate")
    List<Consent> findExpiredConsents(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Check if patient has valid consent for a specific type
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Consent c " +
           "WHERE c.patient.id = :patientId AND c.consentType = :consentType AND c.status = 'GRANTED' " +
           "AND (c.expiryDate IS NULL OR c.expiryDate > :currentDate)")
    boolean hasValidConsent(
        @Param("patientId") UUID patientId,
        @Param("consentType") Consent.ConsentType consentType,
        @Param("currentDate") LocalDateTime currentDate
    );

    /**
     * Count active consents by patient
     */
    @Query("SELECT COUNT(c) FROM Consent c WHERE c.patient.id = :patientId AND c.status = 'GRANTED'")
    long countActiveConsentsByPatientId(@Param("patientId") UUID patientId);
}
