package de.elias.moualem.Anamnesebogen.repository.forms;

import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing FormTranslation entities.
 * Provides CRUD operations and custom queries for form translations.
 */
@Repository
public interface FormTranslationRepository extends JpaRepository<FormTranslation, UUID> {

    /**
     * Find all translations for a specific form definition.
     *
     * @param formDefinition the form definition
     * @return list of translations for the form
     */
    List<FormTranslation> findByFormDefinition(FormDefinition formDefinition);

    /**
     * Find translation for a specific form and language.
     *
     * @param formDefinition the form definition
     * @param language       the language code (e.g., "de", "en", "ar", "ru")
     * @return optional containing the translation if found
     */
    Optional<FormTranslation> findByFormDefinitionAndLanguage(
            FormDefinition formDefinition,
            String language
    );

    /**
     * Find all translations by language code across all forms.
     *
     * @param language the language code
     * @return list of translations in the specified language
     */
    List<FormTranslation> findByLanguage(String language);

    /**
     * Check if a translation exists for a specific form and language.
     *
     * @param formDefinition the form definition
     * @param language       the language code
     * @return true if translation exists
     */
    boolean existsByFormDefinitionAndLanguage(FormDefinition formDefinition, String language);

    /**
     * Delete all translations for a specific form definition.
     * Useful when deleting a form and its associated translations.
     *
     * @param formDefinition the form definition
     */
    void deleteByFormDefinition(FormDefinition formDefinition);

    /**
     * Delete translation for a specific form and language.
     *
     * @param formDefinition the form definition
     * @param language       the language code
     */
    void deleteByFormDefinitionAndLanguage(FormDefinition formDefinition, String language);

    /**
     * Count translations for a specific form definition.
     *
     * @param formDefinition the form definition
     * @return number of translations
     */
    long countByFormDefinition(FormDefinition formDefinition);

    /**
     * Find all translations for a form by form ID.
     * Alternative to findByFormDefinition when you only have the ID.
     *
     * @param formDefinitionId the form definition ID
     * @return list of translations for the form
     */
    @Query("SELECT t FROM FormTranslation t WHERE t.formDefinition.id = :formDefinitionId")
    List<FormTranslation> findByFormDefinitionId(@Param("formDefinitionId") UUID formDefinitionId);
}
