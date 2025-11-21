package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionCreateDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionUpdateDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormStatus;
import de.elias.moualem.Anamnesebogen.repository.forms.FormDefinitionRepository;
import de.elias.moualem.Anamnesebogen.repository.forms.FormTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FormDefinitionService.
 */
@ExtendWith(MockitoExtension.class)
class FormDefinitionServiceTest {

    @Mock
    private FormDefinitionRepository formDefinitionRepository;

    @Mock
    private FormTranslationRepository formTranslationRepository;

    @Mock
    private FormValidationService formValidationService;

    @InjectMocks
    private FormDefinitionService formDefinitionService;

    private ObjectNode testSchema;
    private ObjectNode testUiSchema;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        testSchema = objectMapper.createObjectNode();
        testSchema.put("type", "object");
        testUiSchema = objectMapper.createObjectNode();
        testUiSchema.put("title", "Test Form");
    }

    // ========================================================================
    // Create Form Definition Tests
    // ========================================================================

    @Test
    void createFormDefinition_ShouldCreateFormSuccessfully() {
        // Given
        FormDefinitionCreateDTO createDTO = FormDefinitionCreateDTO.builder()
                .name("Test Form")
                .description("Test Description")
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .createdBy("admin")
                .build();

        FormDefinition savedForm = FormDefinition.builder()
                .id(UUID.randomUUID())
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .category(createDTO.getCategory())
                .version(createDTO.getVersion())
                .status(FormStatus.DRAFT)
                .isActive(false)
                .isDefault(false)
                .schema(createDTO.getSchema())
                .uiSchema(createDTO.getUiSchema())
                .createdBy(createDTO.getCreatedBy())
                .submissions(new ArrayList<>())
                .build();

        when(formDefinitionRepository.save(any(FormDefinition.class))).thenReturn(savedForm);
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        FormDefinitionDTO result = formDefinitionService.createFormDefinition(createDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Form");
        assertThat(result.getCategory()).isEqualTo(FormCategory.ANAMNESIS);
        assertThat(result.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(result.getIsActive()).isFalse();
        verify(formDefinitionRepository, times(1)).save(any(FormDefinition.class));
    }

    @Test
    void createFormDefinition_WithDefaultFlag_ShouldUnsetOtherDefaults() {
        // Given
        FormDefinitionCreateDTO createDTO = FormDefinitionCreateDTO.builder()
                .name("Default Form")
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .isDefault(true)
                .build();

        FormDefinition existingDefault = FormDefinition.builder()
                .id(UUID.randomUUID())
                .name("Old Default")
                .category(FormCategory.ANAMNESIS)
                .isDefault(true)
                .build();

        FormDefinition savedForm = FormDefinition.builder()
                .id(UUID.randomUUID())
                .name(createDTO.getName())
                .category(createDTO.getCategory())
                .version(createDTO.getVersion())
                .status(FormStatus.DRAFT)
                .isActive(false)
                .isDefault(true)
                .schema(createDTO.getSchema())
                .uiSchema(createDTO.getUiSchema())
                .submissions(new ArrayList<>())
                .build();

        when(formDefinitionRepository.findByCategoryAndIsDefault(FormCategory.ANAMNESIS, true))
                .thenReturn(Optional.of(existingDefault));
        when(formDefinitionRepository.save(any(FormDefinition.class))).thenReturn(savedForm);
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        FormDefinitionDTO result = formDefinitionService.createFormDefinition(createDTO);

        // Then
        assertThat(result.getIsDefault()).isTrue();
        verify(formDefinitionRepository, times(2)).save(any(FormDefinition.class)); // Once to unset, once to create
    }

    // ========================================================================
    // Update Form Definition Tests
    // ========================================================================

    @Test
    void updateFormDefinition_ShouldUpdateSuccessfully() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinitionUpdateDTO updateDTO = FormDefinitionUpdateDTO.builder()
                .name("Updated Form")
                .description("Updated Description")
                .version("1.1.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        FormDefinition existingForm = FormDefinition.builder()
                .id(formId)
                .name("Old Name")
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .status(FormStatus.DRAFT)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .submissions(new ArrayList<>())
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(existingForm));
        when(formDefinitionRepository.save(any(FormDefinition.class))).thenReturn(existingForm);
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        FormDefinitionDTO result = formDefinitionService.updateFormDefinition(formId, updateDTO);

        // Then
        assertThat(result.getName()).isEqualTo("Updated Form");
        assertThat(result.getVersion()).isEqualTo("1.1.0");
        verify(formDefinitionRepository, times(1)).save(any(FormDefinition.class));
    }

    @Test
    void updateFormDefinition_PublishedForm_ShouldThrowException() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinitionUpdateDTO updateDTO = FormDefinitionUpdateDTO.builder()
                .name("Updated Form")
                .version("1.1.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        FormDefinition publishedForm = FormDefinition.builder()
                .id(formId)
                .name("Published Form")
                .status(FormStatus.PUBLISHED)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(publishedForm));

        // When / Then
        assertThatThrownBy(() -> formDefinitionService.updateFormDefinition(formId, updateDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update form that is not in DRAFT status");
    }

    @Test
    void updateFormDefinition_NotFound_ShouldThrowException() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinitionUpdateDTO updateDTO = FormDefinitionUpdateDTO.builder()
                .name("Updated Form")
                .version("1.1.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> formDefinitionService.updateFormDefinition(formId, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Form definition not found");
    }

    // ========================================================================
    // Delete Form Definition Tests
    // ========================================================================

    @Test
    void deleteFormDefinition_DraftForm_ShouldDeleteSuccessfully() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinition draftForm = FormDefinition.builder()
                .id(formId)
                .name("Draft Form")
                .status(FormStatus.DRAFT)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(draftForm));

        // When
        formDefinitionService.deleteFormDefinition(formId);

        // Then
        verify(formDefinitionRepository, times(1)).delete(draftForm);
    }

    @Test
    void deleteFormDefinition_PublishedForm_ShouldThrowException() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinition publishedForm = FormDefinition.builder()
                .id(formId)
                .name("Published Form")
                .status(FormStatus.PUBLISHED)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(publishedForm));

        // When / Then
        assertThatThrownBy(() -> formDefinitionService.deleteFormDefinition(formId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete form that is not in DRAFT status");
    }

    // ========================================================================
    // Publish Form Definition Tests
    // ========================================================================

    @Test
    void publishFormDefinition_ShouldPublishSuccessfully() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinition draftForm = FormDefinition.builder()
                .id(formId)
                .name("Draft Form")
                .category(FormCategory.ANAMNESIS)
                .status(FormStatus.DRAFT)
                .isActive(false)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .submissions(new ArrayList<>())
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(draftForm));
        when(formDefinitionRepository.save(any(FormDefinition.class))).thenReturn(draftForm);
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        FormDefinitionDTO result = formDefinitionService.publishFormDefinition(formId, "admin", false);

        // Then
        assertThat(result.getStatus()).isEqualTo(FormStatus.PUBLISHED);
        assertThat(result.getPublishedBy()).isEqualTo("admin");
        verify(formDefinitionRepository, times(1)).save(any(FormDefinition.class));
    }

    @Test
    void publishFormDefinition_WithSetActive_ShouldDeactivateOthers() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinition draftForm = FormDefinition.builder()
                .id(formId)
                .name("Draft Form")
                .category(FormCategory.ANAMNESIS)
                .status(FormStatus.DRAFT)
                .isActive(false)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .submissions(new ArrayList<>())
                .build();

        FormDefinition existingActiveForm = FormDefinition.builder()
                .id(UUID.randomUUID())
                .name("Active Form")
                .category(FormCategory.ANAMNESIS)
                .status(FormStatus.PUBLISHED)
                .isActive(true)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(draftForm));
        when(formDefinitionRepository.findByCategoryAndIsActiveAndStatus(
                FormCategory.ANAMNESIS, true, FormStatus.PUBLISHED))
                .thenReturn(List.of(existingActiveForm));
        when(formDefinitionRepository.save(any(FormDefinition.class))).thenReturn(draftForm);
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        FormDefinitionDTO result = formDefinitionService.publishFormDefinition(formId, "admin", true);

        // Then
        assertThat(result.getStatus()).isEqualTo(FormStatus.PUBLISHED);
        assertThat(result.getIsActive()).isTrue();
        verify(formDefinitionRepository, times(2)).save(any(FormDefinition.class)); // Once to deactivate, once to publish
    }

    // ========================================================================
    // Archive Form Definition Tests
    // ========================================================================

    @Test
    void archiveFormDefinition_ShouldArchiveSuccessfully() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinition publishedForm = FormDefinition.builder()
                .id(formId)
                .name("Published Form")
                .status(FormStatus.PUBLISHED)
                .isActive(true)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .submissions(new ArrayList<>())
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(publishedForm));
        when(formDefinitionRepository.save(any(FormDefinition.class))).thenReturn(publishedForm);
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        FormDefinitionDTO result = formDefinitionService.archiveFormDefinition(formId);

        // Then
        assertThat(result.getStatus()).isEqualTo(FormStatus.ARCHIVED);
        assertThat(result.getIsActive()).isFalse();
        verify(formDefinitionRepository, times(1)).save(any(FormDefinition.class));
    }

    // ========================================================================
    // Get Form Definition Tests
    // ========================================================================

    @Test
    void getFormDefinitionById_ShouldReturnForm() {
        // Given
        UUID formId = UUID.randomUUID();
        FormDefinition form = FormDefinition.builder()
                .id(formId)
                .name("Test Form")
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .status(FormStatus.DRAFT)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .submissions(new ArrayList<>())
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(form));
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        FormDefinitionDTO result = formDefinitionService.getFormDefinitionById(formId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(formId);
        assertThat(result.getName()).isEqualTo("Test Form");
    }

    @Test
    void getActiveFormByCategory_ShouldReturnActiveForm() {
        // Given
        FormDefinition activeForm = FormDefinition.builder()
                .id(UUID.randomUUID())
                .name("Active Form")
                .category(FormCategory.ANAMNESIS)
                .status(FormStatus.PUBLISHED)
                .isActive(true)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .submissions(new ArrayList<>())
                .build();

        when(formDefinitionRepository.findActivePublishedFormByCategory(FormCategory.ANAMNESIS))
                .thenReturn(Optional.of(activeForm));
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        Optional<FormDefinitionDTO> result = formDefinitionService.getActiveFormByCategory(FormCategory.ANAMNESIS);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Active Form");
        assertThat(result.get().getIsActive()).isTrue();
    }

    @Test
    void getAllFormDefinitions_ShouldReturnAllForms() {
        // Given
        List<FormDefinition> forms = List.of(
                FormDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("Form 1")
                        .category(FormCategory.ANAMNESIS)
                        .schema(testSchema)
                        .uiSchema(testUiSchema)
                        .submissions(new ArrayList<>())
                        .build(),
                FormDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("Form 2")
                        .category(FormCategory.CONSENT)
                        .schema(testSchema)
                        .uiSchema(testUiSchema)
                        .submissions(new ArrayList<>())
                        .build()
        );

        when(formDefinitionRepository.findAll()).thenReturn(forms);
        when(formTranslationRepository.findByFormDefinition(any())).thenReturn(new ArrayList<>());

        // When
        List<FormDefinitionDTO> result = formDefinitionService.getAllFormDefinitions();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(FormDefinitionDTO::getName).containsExactlyInAnyOrder("Form 1", "Form 2");
    }
}
