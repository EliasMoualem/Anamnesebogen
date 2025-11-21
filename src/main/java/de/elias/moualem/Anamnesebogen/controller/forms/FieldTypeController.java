package de.elias.moualem.Anamnesebogen.controller.forms;

import de.elias.moualem.Anamnesebogen.dto.forms.FieldTypeDTO;
import de.elias.moualem.Anamnesebogen.entity.FieldType;
import de.elias.moualem.Anamnesebogen.entity.FieldType.FieldCategory;
import de.elias.moualem.Anamnesebogen.service.forms.FieldTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for managing field types.
 * Provides endpoints for retrieving field type registry data for form builder UI.
 */
@RestController
@RequestMapping("/api/field-types")
@RequiredArgsConstructor
@Slf4j
public class FieldTypeController {

    private final FieldTypeService fieldTypeService;

    /**
     * Get all field types ordered by category and field type name.
     * Used to populate field type dropdowns in form builder UI.
     *
     * GET /api/field-types
     *
     * @return list of all field types
     */
    @GetMapping
    public ResponseEntity<List<FieldTypeDTO>> getAllFieldTypes() {
        log.debug("GET /api/field-types - fetching all field types");

        List<FieldType> fieldTypes = fieldTypeService.getAllFieldTypesOrdered();
        List<FieldTypeDTO> fieldTypeDTOs = fieldTypes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.debug("Returning {} field types", fieldTypeDTOs.size());
        return ResponseEntity.ok(fieldTypeDTOs);
    }

    /**
     * Get all required field types.
     * These fields must be mapped for a form to be published.
     *
     * GET /api/field-types/required
     *
     * @return list of required field types
     */
    @GetMapping("/required")
    public ResponseEntity<List<FieldTypeDTO>> getRequiredFieldTypes() {
        log.debug("GET /api/field-types/required - fetching required field types");

        List<FieldType> requiredFields = fieldTypeService.getRequiredFieldTypes();
        List<FieldTypeDTO> fieldTypeDTOs = requiredFields.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.debug("Returning {} required field types", fieldTypeDTOs.size());
        return ResponseEntity.ok(fieldTypeDTOs);
    }

    /**
     * Get field types by category.
     * Used for category-grouped dropdowns in form builder UI.
     *
     * GET /api/field-types/categories/{category}
     *
     * @param category the field category (PERSONAL, CONTACT, INSURANCE, MEDICAL, CONSENT, CUSTOM)
     * @return list of field types in the specified category
     */
    @GetMapping("/categories/{category}")
    public ResponseEntity<List<FieldTypeDTO>> getFieldTypesByCategory(@PathVariable FieldCategory category) {
        log.debug("GET /api/field-types/categories/{} - fetching field types for category", category);

        List<FieldType> fieldTypes = fieldTypeService.getFieldTypesByCategory(category);
        List<FieldTypeDTO> fieldTypeDTOs = fieldTypes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        log.debug("Returning {} field types for category {}", fieldTypeDTOs.size(), category);
        return ResponseEntity.ok(fieldTypeDTOs);
    }

    /**
     * Get all field types grouped by category.
     * Returns a map of category names to lists of field types.
     * Used for rendering category-grouped field type selectors.
     *
     * GET /api/field-types/grouped
     *
     * @return map of categories to field types
     */
    @GetMapping("/grouped")
    public ResponseEntity<java.util.Map<FieldCategory, List<FieldTypeDTO>>> getFieldTypesGrouped() {
        log.debug("GET /api/field-types/grouped - fetching all field types grouped by category");

        List<FieldType> allFieldTypes = fieldTypeService.getAllFieldTypesOrdered();

        // Group by category
        java.util.Map<FieldCategory, List<FieldTypeDTO>> grouped = allFieldTypes.stream()
                .collect(Collectors.groupingBy(
                        FieldType::getCategory,
                        Collectors.mapping(this::toDTO, Collectors.toList())
                ));

        log.debug("Returning field types grouped into {} categories", grouped.size());
        return ResponseEntity.ok(grouped);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Convert FieldType entity to DTO.
     */
    private FieldTypeDTO toDTO(FieldType entity) {
        return FieldTypeDTO.builder()
                .id(entity.getId())
                .fieldType(entity.getFieldType())
                .canonicalName(entity.getCanonicalName())
                .displayNameKey(entity.getDisplayNameKey())
                .category(entity.getCategory())
                .dataType(entity.getDataType())
                .isRequired(entity.getIsRequired())
                .isSystem(entity.getIsSystem())
                .acceptedAliases(entity.getAcceptedAliases())
                .validationRules(entity.getValidationRules())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
