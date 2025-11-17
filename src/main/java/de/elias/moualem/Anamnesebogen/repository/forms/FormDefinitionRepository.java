package de.elias.moualem.Anamnesebogen.repository.forms;

import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing FormDefinition entities.
 * Provides CRUD operations and custom queries for form definitions.
 */
@Repository
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {

    /**
     * Find all forms by status.
     *
     * @param status the form status (DRAFT, PUBLISHED, ARCHIVED)
     * @return list of forms with the specified status
     */
    List<FormDefinition> findByStatus(FormStatus status);

    /**
     * Find all active forms by category.
     * Active forms are those with isActive=true and status=PUBLISHED.
     *
     * @param category the form category
     * @param isActive the active flag
     * @param status   the form status
     * @return list of active forms in the specified category
     */
    List<FormDefinition> findByCategoryAndIsActiveAndStatus(
            FormCategory category,
            Boolean isActive,
            FormStatus status
    );

    /**
     * Find the default form for a specific category.
     * Only one form per category should be marked as default.
     *
     * @param category  the form category
     * @param isDefault the default flag
     * @return optional containing the default form if found
     */
    Optional<FormDefinition> findByCategoryAndIsDefault(
            FormCategory category,
            Boolean isDefault
    );

    /**
     * Find all active forms.
     *
     * @param isActive the active flag
     * @param status   the form status
     * @return list of all active forms
     */
    List<FormDefinition> findByIsActiveAndStatus(Boolean isActive, FormStatus status);

    /**
     * Find all forms by category.
     *
     * @param category the form category
     * @return list of forms in the specified category
     */
    List<FormDefinition> findByCategory(FormCategory category);

    /**
     * Find the active published form for a specific category.
     * Convenience method combining category, active, and published filters.
     *
     * @param category the form category
     * @return optional containing the active form if found
     */
    @Query("SELECT f FROM FormDefinition f WHERE f.category = :category " +
           "AND f.isActive = true AND f.status = 'PUBLISHED' " +
           "ORDER BY f.publishedAt DESC")
    Optional<FormDefinition> findActivePublishedFormByCategory(@Param("category") FormCategory category);

    /**
     * Find all published forms ordered by publish date (newest first).
     *
     * @return list of published forms
     */
    @Query("SELECT f FROM FormDefinition f WHERE f.status = 'PUBLISHED' " +
           "ORDER BY f.publishedAt DESC")
    List<FormDefinition> findAllPublishedOrderedByPublishDate();

    /**
     * Check if another active form exists for the same category.
     * Used to ensure only one form per category is active at a time.
     *
     * @param category the form category
     * @param isActive the active flag
     * @param id       the form ID to exclude from the check
     * @return true if another active form exists
     */
    @Query("SELECT COUNT(f) > 0 FROM FormDefinition f WHERE f.category = :category " +
           "AND f.isActive = :isActive AND f.id != :id")
    boolean existsActiveByCategoryExcludingId(
            @Param("category") FormCategory category,
            @Param("isActive") Boolean isActive,
            @Param("id") UUID id
    );
}
