package de.elias.moualem.Anamnesebogen.controller.forms;

import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionCreateDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionUpdateDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormStatus;
import de.elias.moualem.Anamnesebogen.service.forms.FormDefinitionService;
import de.elias.moualem.Anamnesebogen.service.forms.FormTranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for managing dynamic forms.
 * Provides endpoints for CRUD operations on form definitions and translations.
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@Slf4j
public class FormBuilderController {

    private final FormDefinitionService formDefinitionService;
    private final FormTranslationService formTranslationService;

    // ========================================================================
    // Form Definition Endpoints
    // ========================================================================

    /**
     * Get all form definitions.
     *
     * @return list of all forms
     */
    @GetMapping
    public ResponseEntity<List<FormDefinitionDTO>> getAllForms(
            @RequestParam(required = false) FormCategory category,
            @RequestParam(required = false) FormStatus status
    ) {
        log.debug("GET /api/forms - category: {}, status: {}", category, status);

        List<FormDefinitionDTO> forms;
        if (category != null) {
            forms = formDefinitionService.getFormsByCategory(category);
        } else if (status != null) {
            forms = formDefinitionService.getFormsByStatus(status);
        } else {
            forms = formDefinitionService.getAllFormDefinitions();
        }

        return ResponseEntity.ok(forms);
    }

    /**
     * Get form definition by ID.
     *
     * @param id the form ID
     * @return the form definition
     */
    @GetMapping("/{id}")
    public ResponseEntity<FormDefinitionDTO> getFormById(@PathVariable UUID id) {
        log.debug("GET /api/forms/{}", id);

        try {
            FormDefinitionDTO form = formDefinitionService.getFormDefinitionById(id);
            return ResponseEntity.ok(form);
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get the active published form for a specific category.
     *
     * @param category the form category
     * @return the active form if found
     */
    @GetMapping("/active")
    public ResponseEntity<FormDefinitionDTO> getActiveForm(@RequestParam FormCategory category) {
        log.debug("GET /api/forms/active?category={}", category);

        return formDefinitionService.getActiveFormByCategory(category)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all published forms.
     *
     * @return list of published forms
     */
    @GetMapping("/published")
    public ResponseEntity<List<FormDefinitionDTO>> getAllPublishedForms() {
        log.debug("GET /api/forms/published");

        List<FormDefinitionDTO> forms = formDefinitionService.getAllPublishedForms();
        return ResponseEntity.ok(forms);
    }

    /**
     * Create a new form definition.
     *
     * @param createDTO the form creation data
     * @return the created form
     */
    @PostMapping
    public ResponseEntity<FormDefinitionDTO> createForm(@Valid @RequestBody FormDefinitionCreateDTO createDTO) {
        log.debug("POST /api/forms - name: {}", createDTO.getName());

        try {
            FormDefinitionDTO created = formDefinitionService.createFormDefinition(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating form", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing form definition.
     *
     * @param id        the form ID
     * @param updateDTO the update data
     * @return the updated form
     */
    @PutMapping("/{id}")
    public ResponseEntity<FormDefinitionDTO> updateForm(
            @PathVariable UUID id,
            @Valid @RequestBody FormDefinitionUpdateDTO updateDTO
    ) {
        log.debug("PUT /api/forms/{}", id);

        try {
            FormDefinitionDTO updated = formDefinitionService.updateFormDefinition(id, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Invalid state for update: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a form definition.
     * Only DRAFT forms can be deleted.
     *
     * @param id the form ID
     * @return no content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(@PathVariable UUID id) {
        log.debug("DELETE /api/forms/{}", id);

        try {
            formDefinitionService.deleteFormDefinition(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot delete form: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================================================================
    // Form Lifecycle Endpoints
    // ========================================================================

    /**
     * Publish a form definition.
     *
     * @param id        the form ID
     * @param setActive whether to set this as the active form for its category
     * @return the published form
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<FormDefinitionDTO> publishForm(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean setActive,
            @RequestParam(required = false) String publishedBy
    ) {
        log.debug("POST /api/forms/{}/publish - setActive: {}", id, setActive);

        try {
            FormDefinitionDTO published = formDefinitionService.publishFormDefinition(
                    id,
                    publishedBy != null ? publishedBy : "system",
                    setActive
            );
            return ResponseEntity.ok(published);
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Archive a form definition.
     *
     * @param id the form ID
     * @return the archived form
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<FormDefinitionDTO> archiveForm(@PathVariable UUID id) {
        log.debug("POST /api/forms/{}/archive", id);

        try {
            FormDefinitionDTO archived = formDefinitionService.archiveFormDefinition(id);
            return ResponseEntity.ok(archived);
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a form definition.
     *
     * @param id the form ID
     * @return the deactivated form
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<FormDefinitionDTO> deactivateForm(@PathVariable UUID id) {
        log.debug("POST /api/forms/{}/deactivate", id);

        try {
            FormDefinitionDTO deactivated = formDefinitionService.deactivateFormDefinition(id);
            return ResponseEntity.ok(deactivated);
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activate a form definition.
     *
     * @param id               the form ID
     * @param deactivateOthers whether to deactivate other forms in the same category
     * @return the activated form
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<FormDefinitionDTO> activateForm(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean deactivateOthers
    ) {
        log.debug("POST /api/forms/{}/activate - deactivateOthers: {}", id, deactivateOthers);

        try {
            FormDefinitionDTO activated = formDefinitionService.activateFormDefinition(id, deactivateOthers);
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot activate form: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================================================================
    // Translation Endpoints
    // ========================================================================

    /**
     * Get all translations for a form.
     *
     * @param id the form ID
     * @return list of translations
     */
    @GetMapping("/{id}/translations")
    public ResponseEntity<List<FormTranslationDTO>> getTranslations(@PathVariable UUID id) {
        log.debug("GET /api/forms/{}/translations", id);

        try {
            List<FormTranslationDTO> translations = formTranslationService.getTranslationsByForm(id);
            return ResponseEntity.ok(translations);
        } catch (IllegalArgumentException e) {
            log.error("Form not found: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get translation for a specific language.
     *
     * @param id       the form ID
     * @param language the language code
     * @return the translation
     */
    @GetMapping("/{id}/translations/{language}")
    public ResponseEntity<FormTranslationDTO> getTranslation(
            @PathVariable UUID id,
            @PathVariable String language
    ) {
        log.debug("GET /api/forms/{}/translations/{}", id, language);

        try {
            FormTranslationDTO translation = formTranslationService.getTranslation(id, language);
            return ResponseEntity.ok(translation);
        } catch (IllegalArgumentException e) {
            log.error("Translation not found for form {} in language {}", id, language, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add a new translation to a form.
     *
     * @param id             the form ID
     * @param translationDTO the translation data
     * @return the created translation
     */
    @PostMapping("/{id}/translations")
    public ResponseEntity<FormTranslationDTO> addTranslation(
            @PathVariable UUID id,
            @Valid @RequestBody FormTranslationDTO translationDTO
    ) {
        log.debug("POST /api/forms/{}/translations - language: {}", id, translationDTO.getLanguage());

        try {
            FormTranslationDTO created = formTranslationService.addTranslation(id, translationDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error adding translation", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing translation.
     *
     * @param id             the form ID
     * @param language       the language code
     * @param translationDTO the updated translation data
     * @return the updated translation
     */
    @PutMapping("/{id}/translations/{language}")
    public ResponseEntity<FormTranslationDTO> updateTranslation(
            @PathVariable UUID id,
            @PathVariable String language,
            @Valid @RequestBody FormTranslationDTO translationDTO
    ) {
        log.debug("PUT /api/forms/{}/translations/{}", id, language);

        try {
            FormTranslationDTO updated = formTranslationService.updateTranslation(id, language, translationDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Translation not found for form {} in language {}", id, language, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a translation.
     *
     * @param id       the form ID
     * @param language the language code
     * @return no content if successful
     */
    @DeleteMapping("/{id}/translations/{language}")
    public ResponseEntity<Void> deleteTranslation(
            @PathVariable UUID id,
            @PathVariable String language
    ) {
        log.debug("DELETE /api/forms/{}/translations/{}", id, language);

        try {
            formTranslationService.deleteTranslation(id, language);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Translation not found for form {} in language {}", id, language, e);
            return ResponseEntity.notFound().build();
        }
    }
}
