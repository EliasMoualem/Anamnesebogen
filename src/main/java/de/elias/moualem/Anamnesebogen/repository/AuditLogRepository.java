package de.elias.moualem.Anamnesebogen.repository;

import de.elias.moualem.Anamnesebogen.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity.
 * Provides comprehensive audit trail querying for GDPR Article 30 compliance.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find all audit logs for a specific patient
     */
    @Query("SELECT al FROM AuditLog al WHERE al.patientId = :patientId ORDER BY al.timestamp DESC")
    List<AuditLog> findByPatientId(@Param("patientId") UUID patientId);

    /**
     * Find audit logs by user
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId ORDER BY al.timestamp DESC")
    List<AuditLog> findByUserId(@Param("userId") UUID userId);

    /**
     * Find audit logs by action type
     */
    List<AuditLog> findByActionType(AuditLog.ActionType actionType);

    /**
     * Find audit logs within date range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findByTimestampBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find failed operations
     */
    @Query("SELECT al FROM AuditLog al WHERE al.success = false ORDER BY al.timestamp DESC")
    List<AuditLog> findFailedOperations();

    /**
     * Find security alerts
     */
    @Query("SELECT al FROM AuditLog al WHERE al.securityAlert = true ORDER BY al.timestamp DESC")
    List<AuditLog> findSecurityAlerts();

    /**
     * Find high/critical security level events
     */
    @Query("SELECT al FROM AuditLog al WHERE al.securityLevel IN ('HIGH', 'CRITICAL') " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findHighSecurityEvents();

    /**
     * Find audit logs by entity type and ID
     */
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType AND al.entityId = :entityId " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findByEntityTypeAndEntityId(
        @Param("entityType") String entityType,
        @Param("entityId") UUID entityId
    );

    /**
     * Find recent login attempts for a user
     */
    @Query("SELECT al FROM AuditLog al WHERE al.username = :username AND " +
           "al.actionType IN ('LOGIN', 'LOGIN_FAILED') ORDER BY al.timestamp DESC")
    List<AuditLog> findLoginAttempts(@Param("username") String username);

    /**
     * Find unauthorized access attempts
     */
    @Query("SELECT al FROM AuditLog al WHERE al.actionType = 'UNAUTHORIZED_ACCESS' " +
           "ORDER BY al.timestamp DESC")
    List<AuditLog> findUnauthorizedAccessAttempts();

    /**
     * Find all data access for a patient (for GDPR data access requests)
     */
    @Query("SELECT al FROM AuditLog al WHERE al.patientId = :patientId AND " +
           "al.actionType IN ('VIEW', 'EXPORT', 'DOWNLOAD') ORDER BY al.timestamp DESC")
    List<AuditLog> findDataAccessByPatientId(@Param("patientId") UUID patientId);

    /**
     * Count operations by user
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * Find suspicious activities from an IP address
     */
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress AND " +
           "(al.success = false OR al.securityAlert = true) ORDER BY al.timestamp DESC")
    List<AuditLog> findSuspiciousActivitiesByIp(@Param("ipAddress") String ipAddress);
}
