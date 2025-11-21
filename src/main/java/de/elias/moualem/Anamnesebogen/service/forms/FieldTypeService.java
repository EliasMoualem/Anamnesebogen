package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.elias.moualem.Anamnesebogen.entity.FieldType;
import de.elias.moualem.Anamnesebogen.entity.FieldType.FieldCategory;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.repository.forms.FieldTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing FieldType entities.
 * Handles field type lookups, validation, and mapping operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldTypeService {

    private final FieldTypeRepository fieldTypeRepository;
    private final ObjectMapper objectMapper;

    // ========================================================================
    // Query Operations
    // ========================================================================

    /**
     * Get all field types.
     *
     * @return list of all field types
     */
    @Transactional(readOnly = true)
    public List<FieldType> getAllFieldTypes() {
        log.debug("Fetching all field types");
        return fieldTypeRepository.findAll();
    }

    /**
     * Get all field types ordered by category and field type name.
     * Useful for UI dropdowns in form builder.
     *
     * @return list of all field types ordered by category
     */
    @Transactional(readOnly = true)
    public List<FieldType> getAllFieldTypesOrdered() {
        log.debug("Fetching all field types ordered by category");
        return fieldTypeRepository.findAllOrderedByCategoryAndFieldType();
    }

    /**
     * Get all required field types.
     * These fields must be mapped for a form to be published.
     *
     * @return list of required field types
     */
    @Transactional(readOnly = true)
    public List<FieldType> getRequiredFieldTypes() {
        log.debug("Fetching required field types");
        return fieldTypeRepository.findByIsRequired(true);
    }

    /**
     * Get field types by category.
     *
     * @param category the field category
     * @return list of field types in the category
     */
    @Transactional(readOnly = true)
    public List<FieldType> getFieldTypesByCategory(FieldCategory category) {
        log.debug("Fetching field types for category: {}", category);
        return fieldTypeRepository.findByCategoryOrderedByFieldType(category);
    }

    /**
     * Find a field type by its unique identifier.
     *
     * @param fieldType the field type identifier (e.g., "FIRST_NAME")
     * @return optional containing the field type if found
     */
    @Transactional(readOnly = true)
    public Optional<FieldType> findByFieldType(String fieldType) {
        log.debug("Looking up field type: {}", fieldType);
        return fieldTypeRepository.findByFieldType(fieldType);
    }

    /**
     * Find a field type by its canonical name.
     *
     * @param canonicalName the canonical name (e.g., "firstName")
     * @return optional containing the field type if found
     */
    @Transactional(readOnly = true)
    public Optional<FieldType> findByCanonicalName(String canonicalName) {
        log.debug("Looking up field type by canonical name: {}", canonicalName);
        return fieldTypeRepository.findByCanonicalName(canonicalName);
    }

    /**
     * Find a field type by an alias (alternative field name).
     * Searches through accepted_aliases JSONB arrays to find matching field type.
     *
     * @param alias the field name alias (e.g., "vorname", "first_name")
     * @return optional containing the field type if found
     */
    @Transactional(readOnly = true)
    public Optional<FieldType> findByAlias(String alias) {
        log.debug("Searching for field type by alias: {}", alias);

        if (alias == null || alias.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedAlias = alias.trim().toLowerCase();

        // Search through all field types
        return fieldTypeRepository.findAll().stream()
                .filter(fieldType -> {
                    JsonNode aliases = fieldType.getAcceptedAliases();
                    if (aliases == null || !aliases.isArray()) {
                        return false;
                    }

                    // Check if alias matches any accepted alias (case-insensitive)
                    for (JsonNode aliasNode : aliases) {
                        String acceptedAlias = aliasNode.asText().toLowerCase();
                        if (acceptedAlias.equals(normalizedAlias)) {
                            log.debug("Found field type {} for alias {}", fieldType.getFieldType(), alias);
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst();
    }

    // ========================================================================
    // Validation Operations
    // ========================================================================

    /**
     * Validate that all required field types are mapped in a form definition.
     *
     * @param formDefinition the form definition to validate
     * @return list of validation error messages (empty if valid)
     */
    @Transactional(readOnly = true)
    public List<String> validateRequiredFieldMappings(FormDefinition formDefinition) {
        log.debug("Validating required field mappings for form: {}", formDefinition.getId());

        List<String> errors = new ArrayList<>();
        JsonNode fieldMappings = formDefinition.getFieldMappings();

        if (fieldMappings == null || fieldMappings.isEmpty() || fieldMappings.isNull()) {
            errors.add("Form has no field mappings defined");
            return errors;
        }

        List<FieldType> requiredFields = getRequiredFieldTypes();

        // Check each required field type
        for (FieldType requiredField : requiredFields) {
            boolean isMapped = false;

            // Iterate through field mappings to find if this required field is mapped
            Iterator<Map.Entry<String, JsonNode>> mappingIterator = fieldMappings.fields();
            while (mappingIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = mappingIterator.next();
                String mappedFieldType = entry.getValue().asText();

                if (mappedFieldType.equals(requiredField.getFieldType())) {
                    isMapped = true;
                    break;
                }
            }

            if (!isMapped) {
                errors.add(String.format(
                    "Required field type '%s' (%s) is not mapped. Please add a field for: %s",
                    requiredField.getFieldType(),
                    requiredField.getCanonicalName(),
                    requiredField.getDisplayNameKey()
                ));
            }
        }

        if (errors.isEmpty()) {
            log.debug("All required field types are mapped for form: {}", formDefinition.getId());
        } else {
            log.warn("Form {} has {} missing required field mappings", formDefinition.getId(), errors.size());
        }

        return errors;
    }

    /**
     * Check if a specific field type is mapped in a form definition.
     *
     * @param formDefinition the form definition
     * @param fieldType      the field type identifier (e.g., "FIRST_NAME")
     * @return true if the field type is mapped
     */
    @Transactional(readOnly = true)
    public boolean isFieldTypeMapped(FormDefinition formDefinition, String fieldType) {
        JsonNode fieldMappings = formDefinition.getFieldMappings();

        if (fieldMappings == null || fieldMappings.isEmpty() || fieldMappings.isNull()) {
            return false;
        }

        Iterator<Map.Entry<String, JsonNode>> mappingIterator = fieldMappings.fields();
        while (mappingIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = mappingIterator.next();
            if (entry.getValue().asText().equals(fieldType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the canonical name for a schema field name using field mappings.
     *
     * @param formDefinition the form definition containing field mappings
     * @param schemaFieldName the field name as it appears in the form schema
     * @return optional containing the canonical name (Patient property) if mapped
     */
    @Transactional(readOnly = true)
    public Optional<String> getCanonicalNameForSchemaField(FormDefinition formDefinition, String schemaFieldName) {
        JsonNode fieldMappings = formDefinition.getFieldMappings();

        if (fieldMappings == null || !fieldMappings.has(schemaFieldName)) {
            return Optional.empty();
        }

        String fieldType = fieldMappings.get(schemaFieldName).asText();
        return findByFieldType(fieldType)
                .map(FieldType::getCanonicalName);
    }

    /**
     * Get all mapped schema field names for a form definition.
     *
     * @param formDefinition the form definition
     * @return map of schema field names to canonical names
     */
    @Transactional(readOnly = true)
    public Map<String, String> getMappedFields(FormDefinition formDefinition) {
        Map<String, String> mappedFields = new HashMap<>();
        JsonNode fieldMappings = formDefinition.getFieldMappings();

        if (fieldMappings == null || fieldMappings.isEmpty() || fieldMappings.isNull()) {
            return mappedFields;
        }

        Iterator<Map.Entry<String, JsonNode>> mappingIterator = fieldMappings.fields();
        while (mappingIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = mappingIterator.next();
            String schemaFieldName = entry.getKey();
            String fieldType = entry.getValue().asText();

            findByFieldType(fieldType).ifPresent(ft ->
                mappedFields.put(schemaFieldName, ft.getCanonicalName())
            );
        }

        return mappedFields;
    }

    // ========================================================================
    // CRUD Operations (for custom field types)
    // ========================================================================

    /**
     * Create a custom field type.
     * System field types are seeded via migration and cannot be created via API.
     *
     * @param fieldType the field type to create
     * @return the saved field type
     * @throws IllegalArgumentException if field type or canonical name already exists
     */
    @Transactional
    public FieldType createCustomFieldType(FieldType fieldType) {
        log.debug("Creating custom field type: {}", fieldType.getFieldType());

        // Validate uniqueness
        if (fieldTypeRepository.existsByFieldType(fieldType.getFieldType())) {
            throw new IllegalArgumentException(
                "Field type already exists: " + fieldType.getFieldType()
            );
        }

        if (fieldTypeRepository.existsByCanonicalName(fieldType.getCanonicalName())) {
            throw new IllegalArgumentException(
                "Canonical name already exists: " + fieldType.getCanonicalName()
            );
        }

        // Set as custom (non-system) field
        fieldType.setIsSystem(false);

        FieldType saved = fieldTypeRepository.save(fieldType);
        log.info("Created custom field type: {} ({})", saved.getFieldType(), saved.getCanonicalName());

        return saved;
    }

    /**
     * Delete a custom field type.
     * System field types cannot be deleted.
     *
     * @param id the field type ID
     * @throws IllegalArgumentException if field type not found
     * @throws IllegalStateException    if attempting to delete a system field type
     */
    @Transactional
    public void deleteCustomFieldType(UUID id) {
        log.debug("Deleting custom field type: {}", id);

        FieldType fieldType = fieldTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Field type not found: " + id));

        if (fieldType.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system field type: " + fieldType.getFieldType());
        }

        fieldTypeRepository.delete(fieldType);
        log.info("Deleted custom field type: {}", fieldType.getFieldType());
    }
}
