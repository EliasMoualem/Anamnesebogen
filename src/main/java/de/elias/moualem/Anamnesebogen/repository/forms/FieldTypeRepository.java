package de.elias.moualem.Anamnesebogen.repository.forms;

import de.elias.moualem.Anamnesebogen.entity.FieldType;
import de.elias.moualem.Anamnesebogen.entity.FieldType.FieldCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing FieldType entities.
 * Provides CRUD operations and custom queries for field type registry.
 */
@Repository
public interface FieldTypeRepository extends JpaRepository<FieldType, UUID> {

    /**
     * Find a field type by its unique field type identifier.
     *
     * @param fieldType the field type identifier (e.g., "FIRST_NAME", "EMAIL")
     * @return optional containing the field type if found
     */
    Optional<FieldType> findByFieldType(String fieldType);

    /**
     * Find a field type by its canonical name (Patient entity property).
     *
     * @param canonicalName the canonical name (e.g., "firstName", "email")
     * @return optional containing the field type if found
     */
    Optional<FieldType> findByCanonicalName(String canonicalName);

    /**
     * Find all field types in a specific category.
     * Used for grouping fields in UI dropdowns.
     *
     * @param category the field category (PERSONAL, CONTACT, INSURANCE, MEDICAL, CONSENT, CUSTOM)
     * @return list of field types in the specified category
     */
    List<FieldType> findByCategory(FieldCategory category);

    /**
     * Find all required field types.
     * These fields must be mapped for a form to be published.
     *
     * @param isRequired the required flag
     * @return list of required field types
     */
    List<FieldType> findByIsRequired(Boolean isRequired);

    /**
     * Find all system field types.
     * System fields are predefined and cannot be deleted.
     *
     * @param isSystem the system flag
     * @return list of system field types
     */
    List<FieldType> findByIsSystem(Boolean isSystem);

    /**
     * Find all field types ordered by category and field type name.
     * Useful for UI display in form builder dropdowns.
     *
     * @return list of all field types ordered by category and field type
     */
    @Query("SELECT f FROM FieldType f ORDER BY f.category ASC, f.fieldType ASC")
    List<FieldType> findAllOrderedByCategoryAndFieldType();

    /**
     * Find field types by category ordered by field type name.
     * Used for populating category-grouped dropdowns in form builder.
     *
     * @param category the field category
     * @return list of field types in the category, ordered alphabetically
     */
    @Query("SELECT f FROM FieldType f WHERE f.category = :category ORDER BY f.fieldType ASC")
    List<FieldType> findByCategoryOrderedByFieldType(@Param("category") FieldCategory category);

    /**
     * Check if a field type with the given identifier already exists.
     * Used to prevent duplicate field type registrations.
     *
     * @param fieldType the field type identifier
     * @return true if a field type with this identifier exists
     */
    boolean existsByFieldType(String fieldType);

    /**
     * Check if a field type with the given canonical name already exists.
     * Used to prevent multiple field types mapping to the same Patient property.
     *
     * @param canonicalName the canonical name
     * @return true if a field type with this canonical name exists
     */
    boolean existsByCanonicalName(String canonicalName);
}
