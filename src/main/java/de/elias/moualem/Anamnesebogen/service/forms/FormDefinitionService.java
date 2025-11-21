package de.elias.moualem.Anamnesebogen.service.forms;

import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionCreateDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionUpdateDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormStatus;
import de.elias.moualem.Anamnesebogen.entity.FormTranslation;
import de.elias.moualem.Anamnesebogen.repository.forms.FormDefinitionRepository;
import de.elias.moualem.Anamnesebogen.repository.forms.FormTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing FormDefinition entities.
 * Handles CRUD operations, lifecycle management, and business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FormDefinitionService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormTranslationRepository formTranslationRepository;
    private final FormValidationService formValidationService;

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    /**
     * Create a new form definition.
     *
     * @param createDTO the form creation data
     * @return the created form as DTO
     */
    @Transactional
    public FormDefinitionDTO createFormDefinition(FormDefinitionCreateDTO createDTO) {
        log.debug("Creating new form definition: {}", createDTO.getName());

        // Create entity from DTO
        FormDefinition formDefinition = FormDefinition.builder()
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .category(createDTO.getCategory())
                .version(createDTO.getVersion())
                .status(FormStatus.DRAFT)
                .isActive(false)
                .isDefault(createDTO.getIsDefault() != null && createDTO.getIsDefault())
                .schema(createDTO.getSchema())
                .uiSchema(createDTO.getUiSchema())
                .validationRules(createDTO.getValidationRules())
                .renderingOptions(createDTO.getRenderingOptions())
                .fieldMappings(createDTO.getFieldMappings())
                .createdBy(createDTO.getCreatedBy())
                .build();

        // If this form is set as default, unset other defaults in the same category
        if (formDefinition.getIsDefault()) {
            unsetDefaultForCategory(formDefinition.getCategory());
        }

        // Save to database
        FormDefinition saved = formDefinitionRepository.save(formDefinition);
        log.info("Created form definition with ID: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * Update an existing form definition.
     *
     * @param id        the form ID
     * @param updateDTO the update data
     * @return the updated form as DTO
     * @throws IllegalArgumentException if form not found
     */
    @Transactional
    public FormDefinitionDTO updateFormDefinition(UUID id, FormDefinitionUpdateDTO updateDTO) {
        log.debug("Updating form definition: {}", id);

        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));

        // Only allow updates to DRAFT forms
        if (formDefinition.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Cannot update form that is not in DRAFT status. " +
                    "Current status: " + formDefinition.getStatus());
        }

        // Update fields
        formDefinition.setName(updateDTO.getName());
        formDefinition.setDescription(updateDTO.getDescription());
        formDefinition.setVersion(updateDTO.getVersion());
        formDefinition.setSchema(updateDTO.getSchema());
        formDefinition.setUiSchema(updateDTO.getUiSchema());
        formDefinition.setValidationRules(updateDTO.getValidationRules());
        formDefinition.setRenderingOptions(updateDTO.getRenderingOptions());

        // Update field mappings if provided
        if (updateDTO.getFieldMappings() != null) {
            formDefinition.setFieldMappings(updateDTO.getFieldMappings());
        }

        // Handle default flag
        if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault() && !formDefinition.getIsDefault()) {
            unsetDefaultForCategory(formDefinition.getCategory());
            formDefinition.setIsDefault(true);
        } else if (updateDTO.getIsDefault() != null && !updateDTO.getIsDefault()) {
            formDefinition.setIsDefault(false);
        }

        FormDefinition updated = formDefinitionRepository.save(formDefinition);
        log.info("Updated form definition: {}", id);

        return toDTO(updated);
    }

    /**
     * Delete a form definition.
     * Only DRAFT forms can be deleted.
     *
     * @param id the form ID
     * @throws IllegalArgumentException if form not found
     * @throws IllegalStateException    if form is not in DRAFT status
     */
    @Transactional
    public void deleteFormDefinition(UUID id) {
        log.debug("Deleting form definition: {}", id);

        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));


        formDefinitionRepository.delete(formDefinition);
        log.info("Deleted form definition: {}", id);
    }

    /**
     * Get form definition by ID.
     *
     * @param id the form ID
     * @return the form as DTO
     * @throws IllegalArgumentException if form not found
     */
    @Transactional(readOnly = true)
    public FormDefinitionDTO getFormDefinitionById(UUID id) {
        log.debug("Fetching form definition: {}", id);

        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));

        return toDTO(formDefinition);
    }

    /**
     * Get all form definitions.
     *
     * @return list of all forms as DTOs
     */
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> getAllFormDefinitions() {
        log.debug("Fetching all form definitions");

        return formDefinitionRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all forms by category.
     *
     * @param category the form category
     * @return list of forms in the category
     */
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> getFormsByCategory(FormCategory category) {
        log.debug("Fetching forms by category: {}", category);

        return formDefinitionRepository.findByCategory(category).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all forms by status.
     *
     * @param status the form status
     * @return list of forms with the status
     */
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> getFormsByStatus(FormStatus status) {
        log.debug("Fetching forms by status: {}", status);

        return formDefinitionRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get form definition entity by ID (for internal use with template
     generation).
     *
     * @param id the form ID
     * @return the form entity
     * @throws IllegalArgumentException if form not found
     */
    @Transactional(readOnly = true)
    public FormDefinition getFormById(UUID id) {
        log.debug("Fetching form entity: {}", id);
        return formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));
    }

    // ========================================================================
    // Lifecycle Management
    // ========================================================================

    /**
     * Publish a form definition.
     * Makes the form available for use and optionally sets it as the active form for its category.
     *
     * @param id          the form ID
     * @param publishedBy the user publishing the form
     * @param setActive   whether to set this as the active form for its category
     * @return the published form as DTO
     * @throws IllegalArgumentException if form not found
     */
    @Transactional
    public FormDefinitionDTO publishFormDefinition(UUID id, String publishedBy, boolean setActive) {
        log.debug("Publishing form definition: {}", id);

        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));

        // Validate field mappings before publishing
        formValidationService.validateFieldMappings(formDefinition);

        // Publish the form
        formDefinition.publish(publishedBy);

        // If setActive is true, deactivate other forms in the same category
        if (setActive) {
            deactivateOtherFormsInCategory(formDefinition.getCategory(), id);
            formDefinition.setIsActive(true);
        }

        FormDefinition published = formDefinitionRepository.save(formDefinition);
        log.info("Published form definition: {}", id);

        return toDTO(published);
    }

    /**
     * Archive a form definition.
     * Marks the form as ARCHIVED and deactivates it.
     *
     * @param id the form ID
     * @return the archived form as DTO
     * @throws IllegalArgumentException if form not found
     */
    @Transactional
    public FormDefinitionDTO archiveFormDefinition(UUID id) {
        log.debug("Archiving form definition: {}", id);

        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));

        formDefinition.archive();
        FormDefinition archived = formDefinitionRepository.save(formDefinition);
        log.info("Archived form definition: {}", id);

        return toDTO(archived);
    }

    /**
     * Deactivate a form definition.
     * Makes the form inactive without changing its status.
     *
     * @param id the form ID
     * @return the deactivated form as DTO
     * @throws IllegalArgumentException if form not found
     */
    @Transactional
    public FormDefinitionDTO deactivateFormDefinition(UUID id) {
        log.debug("Deactivating form definition: {}", id);

        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));

        formDefinition.deactivate();
        FormDefinition deactivated = formDefinitionRepository.save(formDefinition);
        log.info("Deactivated form definition: {}", id);

        return toDTO(deactivated);
    }

    /**
     * Activate a published form definition.
     * Sets the form as active and optionally deactivates other forms in the same category.
     *
     * @param id                 the form ID
     * @param deactivateOthers   whether to deactivate other forms in the same category
     * @return the activated form as DTO
     * @throws IllegalArgumentException if form not found
     * @throws IllegalStateException    if form is not published
     */
    @Transactional
    public FormDefinitionDTO activateFormDefinition(UUID id, boolean deactivateOthers) {
        log.debug("Activating form definition: {}", id);

        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Form definition not found: " + id));

        // Only published forms can be activated
        if (formDefinition.getStatus() != FormStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot activate form that is not PUBLISHED. " +
                    "Current status: " + formDefinition.getStatus());
        }

        if (deactivateOthers) {
            deactivateOtherFormsInCategory(formDefinition.getCategory(), id);
        }

        formDefinition.setIsActive(true);
        formDefinition.setUpdatedAt(LocalDateTime.now());
        FormDefinition activated = formDefinitionRepository.save(formDefinition);
        log.info("Activated form definition: {}", id);

        return toDTO(activated);
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Get the active published form for a specific category.
     *
     * @param category the form category
     * @return optional containing the active form if found
     */
    @Transactional(readOnly = true)
    public Optional<FormDefinitionDTO> getActiveFormByCategory(FormCategory category) {
        log.debug("Fetching active form for category: {}", category);

        return formDefinitionRepository.findActivePublishedFormByCategory(category)
                .map(this::toDTO);
    }

    /**
     * Get all published forms ordered by publish date (newest first).
     *
     * @return list of published forms
     */
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> getAllPublishedForms() {
        log.debug("Fetching all published forms");

        return formDefinitionRepository.findAllPublishedOrderedByPublishDate().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Unset the default flag for all forms in a category.
     * Used when setting a new default form.
     *
     * @param category the form category
     */
    private void unsetDefaultForCategory(FormCategory category) {
        log.debug("Unsetting default for category: {}", category);

        formDefinitionRepository.findByCategoryAndIsDefault(category, true)
                .ifPresent(form -> {
                    form.setIsDefault(false);
                    formDefinitionRepository.save(form);
                });
    }

    /**
     * Deactivate all other forms in a category except the specified form.
     * Used when activating a new form.
     *
     * @param category the form category
     * @param exceptId the form ID to exclude from deactivation
     */
    private void deactivateOtherFormsInCategory(FormCategory category, UUID exceptId) {
        log.debug("Deactivating other forms in category: {}", category);

        List<FormDefinition> activeForms = formDefinitionRepository
                .findByCategoryAndIsActiveAndStatus(category, true, FormStatus.PUBLISHED);

        activeForms.stream()
                .filter(form -> !form.getId().equals(exceptId))
                .forEach(form -> {
                    form.setIsActive(false);
                    form.setUpdatedAt(LocalDateTime.now());
                    formDefinitionRepository.save(form);
                });
    }

    /**
     * Convert FormDefinition entity to DTO.
     *
     * @param entity the form entity
     * @return the form DTO
     */
    private FormDefinitionDTO toDTO(FormDefinition entity) {
        // Fetch translations
        List<FormTranslation> translations = formTranslationRepository.findByFormDefinition(entity);

        // Convert translations to DTOs
        List<FormTranslationDTO> translationDTOs = translations.stream()
                .map(t -> FormTranslationDTO.builder()
                        .id(t.getId())
                        .language(t.getLanguage())
                        .translations(t.getTranslations())
                        .languageDisplayName(t.getLanguageDisplayName())
                        .isRtl(t.isRtl())
                        .createdAt(t.getCreatedAt())
                        .updatedAt(t.getUpdatedAt())
                        .createdBy(t.getCreatedBy())
                        .build())
                .collect(Collectors.toList());

        // Get submission count
        Long submissionCount = (long) entity.getSubmissions().size();

        return FormDefinitionDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .version(entity.getVersion())
                .status(entity.getStatus())
                .isActive(entity.getIsActive())
                .isDefault(entity.getIsDefault())
                .schema(entity.getSchema())
                .uiSchema(entity.getUiSchema())
                .validationRules(entity.getValidationRules())
                .renderingOptions(entity.getRenderingOptions())
                .fieldMappings(entity.getFieldMappings())
                .translations(translationDTOs)
                .submissionCount(submissionCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .publishedAt(entity.getPublishedAt())
                .publishedBy(entity.getPublishedBy())
                .build();
    }
}
