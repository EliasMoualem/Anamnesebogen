package de.elias.moualem.Anamnesebogen.service.forms;

import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormTranslation;
import de.elias.moualem.Anamnesebogen.repository.forms.FormDefinitionRepository;
import de.elias.moualem.Anamnesebogen.repository.forms.FormTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing FormTranslation entities.
 * Handles CRUD operations for form translations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormTranslationService {

    private final FormTranslationRepository formTranslationRepository;
    private final FormDefinitionRepository formDefinitionRepository;

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    /**
     * Add a translation to a form definition.
     *
     * @param formDefinitionId the form definition ID
     * @param translationDTO   the translation data
     * @return the created translation as DTO
     * @throws IllegalArgumentException if form not found or translation already exists
     */
    @Transactional
    public FormTranslationDTO addTranslation(UUID formDefinitionId, FormTranslationDTO translationDTO) {
        log.debug("Adding translation for form: {} in language: {}",
                formDefinitionId, translationDTO.getLanguage());

        // Fetch form definition
        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + formDefinitionId));

        // Check if translation already exists
        if (formTranslationRepository.existsByFormDefinitionAndLanguage(
                formDefinition, translationDTO.getLanguage())) {
            throw new IllegalArgumentException("Translation already exists for language: " +
                    translationDTO.getLanguage());
        }

        // Validate language code
        if (!FormTranslation.Language.isSupported(translationDTO.getLanguage())) {
            throw new IllegalArgumentException("Unsupported language code: " + translationDTO.getLanguage());
        }

        // Create translation entity
        FormTranslation translation = FormTranslation.builder()
                .formDefinition(formDefinition)
                .language(translationDTO.getLanguage())
                .translations(translationDTO.getTranslations())
                .createdBy(translationDTO.getCreatedBy())
                .build();

        // Save to database
        FormTranslation saved = formTranslationRepository.save(translation);
        log.info("Added translation for form: {} in language: {}", formDefinitionId, translationDTO.getLanguage());

        return toDTO(saved);
    }

    /**
     * Update an existing translation.
     *
     * @param formDefinitionId the form definition ID
     * @param language         the language code
     * @param translationDTO   the updated translation data
     * @return the updated translation as DTO
     * @throws IllegalArgumentException if form or translation not found
     */
    @Transactional
    public FormTranslationDTO updateTranslation(UUID formDefinitionId, String language,
                                                FormTranslationDTO translationDTO) {
        log.debug("Updating translation for form: {} in language: {}", formDefinitionId, language);

        // Fetch form definition
        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + formDefinitionId));

        // Fetch translation
        FormTranslation translation = formTranslationRepository
                .findByFormDefinitionAndLanguage(formDefinition, language)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Translation not found for language: " + language));

        // Update translation data
        translation.setTranslations(translationDTO.getTranslations());

        // Save to database
        FormTranslation updated = formTranslationRepository.save(translation);
        log.info("Updated translation for form: {} in language: {}", formDefinitionId, language);

        return toDTO(updated);
    }

    /**
     * Delete a translation.
     *
     * @param formDefinitionId the form definition ID
     * @param language         the language code
     * @throws IllegalArgumentException if form or translation not found
     */
    @Transactional
    public void deleteTranslation(UUID formDefinitionId, String language) {
        log.debug("Deleting translation for form: {} in language: {}", formDefinitionId, language);

        // Fetch form definition
        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + formDefinitionId));

        // Fetch translation
        FormTranslation translation = formTranslationRepository
                .findByFormDefinitionAndLanguage(formDefinition, language)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Translation not found for language: " + language));

        // Delete translation
        formTranslationRepository.delete(translation);
        log.info("Deleted translation for form: {} in language: {}", formDefinitionId, language);
    }

    /**
     * Get translation by form and language.
     *
     * @param formDefinitionId the form definition ID
     * @param language         the language code
     * @return the translation as DTO
     * @throws IllegalArgumentException if form or translation not found
     */
    @Transactional(readOnly = true)
    public FormTranslationDTO getTranslation(UUID formDefinitionId, String language) {
        log.debug("Fetching translation for form: {} in language: {}", formDefinitionId, language);

        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + formDefinitionId));

        FormTranslation translation = formTranslationRepository
                .findByFormDefinitionAndLanguage(formDefinition, language)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Translation not found for language: " + language));

        return toDTO(translation);
    }

    /**
     * Get all translations for a form definition.
     *
     * @param formDefinitionId the form definition ID
     * @return list of translations as DTOs
     * @throws IllegalArgumentException if form not found
     */
    @Transactional(readOnly = true)
    public List<FormTranslationDTO> getTranslationsByForm(UUID formDefinitionId) {
        log.debug("Fetching all translations for form: {}", formDefinitionId);

        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + formDefinitionId));

        return formTranslationRepository.findByFormDefinition(formDefinition).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all translations in a specific language across all forms.
     *
     * @param language the language code
     * @return list of translations in the specified language
     */
    @Transactional(readOnly = true)
    public List<FormTranslationDTO> getTranslationsByLanguage(String language) {
        log.debug("Fetching all translations in language: {}", language);

        return formTranslationRepository.findByLanguage(language).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a translation exists for a form and language.
     *
     * @param formDefinitionId the form definition ID
     * @param language         the language code
     * @return true if translation exists
     */
    @Transactional(readOnly = true)
    public boolean translationExists(UUID formDefinitionId, String language) {
        log.debug("Checking if translation exists for form: {} in language: {}", formDefinitionId, language);

        Optional<FormDefinition> formDefinition = formDefinitionRepository.findById(formDefinitionId);
        if (formDefinition.isEmpty()) {
            return false;
        }

        return formTranslationRepository.existsByFormDefinitionAndLanguage(formDefinition.get(), language);
    }

    /**
     * Count translations for a form.
     *
     * @param formDefinitionId the form definition ID
     * @return number of translations
     * @throws IllegalArgumentException if form not found
     */
    @Transactional(readOnly = true)
    public long countTranslations(UUID formDefinitionId) {
        log.debug("Counting translations for form: {}", formDefinitionId);

        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + formDefinitionId));

        return formTranslationRepository.countByFormDefinition(formDefinition);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Convert FormTranslation entity to DTO.
     *
     * @param entity the translation entity
     * @return the translation DTO
     */
    private FormTranslationDTO toDTO(FormTranslation entity) {
        return FormTranslationDTO.builder()
                .id(entity.getId())
                .language(entity.getLanguage())
                .translations(entity.getTranslations())
                .languageDisplayName(entity.getLanguageDisplayName())
                .isRtl(entity.isRtl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
