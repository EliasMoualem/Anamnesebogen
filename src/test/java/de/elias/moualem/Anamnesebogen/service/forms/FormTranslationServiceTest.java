package de.elias.moualem.Anamnesebogen.service.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import de.elias.moualem.Anamnesebogen.entity.FormTranslation;
import de.elias.moualem.Anamnesebogen.repository.forms.FormDefinitionRepository;
import de.elias.moualem.Anamnesebogen.repository.forms.FormTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FormTranslationService.
 */
@ExtendWith(MockitoExtension.class)
class FormTranslationServiceTest {

    @Mock
    private FormTranslationRepository formTranslationRepository;

    @Mock
    private FormDefinitionRepository formDefinitionRepository;

    @InjectMocks
    private FormTranslationService formTranslationService;

    private ObjectMapper objectMapper;
    private FormDefinition testForm;
    private ObjectNode testTranslations;
    private UUID formId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        formId = UUID.randomUUID();

        testForm = FormDefinition.builder()
                .id(formId)
                .name("Test Form")
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .build();

        testTranslations = objectMapper.createObjectNode();
        ObjectNode fields = objectMapper.createObjectNode();
        fields.put("firstName", "Vorname");
        fields.put("lastName", "Nachname");
        testTranslations.set("fields", fields);
    }

    // ========================================================================
    // Add Translation Tests
    // ========================================================================

    @Test
    void addTranslation_ShouldAddSuccessfully() {
        // Given
        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("de")
                .translations(testTranslations)
                .createdBy("admin")
                .build();

        FormTranslation savedTranslation = FormTranslation.builder()
                .id(UUID.randomUUID())
                .formDefinition(testForm)
                .language("de")
                .translations(testTranslations)
                .createdBy("admin")
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.existsByFormDefinitionAndLanguage(testForm, "de")).thenReturn(false);
        when(formTranslationRepository.save(any(FormTranslation.class))).thenReturn(savedTranslation);

        // When
        FormTranslationDTO result = formTranslationService.addTranslation(formId, translationDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("de");
        assertThat(result.getCreatedBy()).isEqualTo("admin");
        verify(formTranslationRepository, times(1)).save(any(FormTranslation.class));
    }

    @Test
    void addTranslation_FormNotFound_ShouldThrowException() {
        // Given
        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("de")
                .translations(testTranslations)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> formTranslationService.addTranslation(formId, translationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Form definition not found");
    }

    @Test
    void addTranslation_TranslationAlreadyExists_ShouldThrowException() {
        // Given
        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("de")
                .translations(testTranslations)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.existsByFormDefinitionAndLanguage(testForm, "de")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> formTranslationService.addTranslation(formId, translationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Translation already exists");
    }

    @Test
    void addTranslation_UnsupportedLanguage_ShouldThrowException() {
        // Given
        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("fr") // French is not supported
                .translations(testTranslations)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.existsByFormDefinitionAndLanguage(testForm, "fr")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> formTranslationService.addTranslation(formId, translationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported language code");
    }

    // ========================================================================
    // Update Translation Tests
    // ========================================================================

    @Test
    void updateTranslation_ShouldUpdateSuccessfully() {
        // Given
        ObjectNode updatedTranslations = objectMapper.createObjectNode();
        ObjectNode fields = objectMapper.createObjectNode();
        fields.put("firstName", "Vorname (aktualisiert)");
        updatedTranslations.set("fields", fields);

        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("de")
                .translations(updatedTranslations)
                .build();

        FormTranslation existingTranslation = FormTranslation.builder()
                .id(UUID.randomUUID())
                .formDefinition(testForm)
                .language("de")
                .translations(testTranslations)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.findByFormDefinitionAndLanguage(testForm, "de"))
                .thenReturn(Optional.of(existingTranslation));
        when(formTranslationRepository.save(any(FormTranslation.class))).thenReturn(existingTranslation);

        // When
        FormTranslationDTO result = formTranslationService.updateTranslation(formId, "de", translationDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("de");
        verify(formTranslationRepository, times(1)).save(any(FormTranslation.class));
    }

    @Test
    void updateTranslation_TranslationNotFound_ShouldThrowException() {
        // Given
        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("de")
                .translations(testTranslations)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.findByFormDefinitionAndLanguage(testForm, "de"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> formTranslationService.updateTranslation(formId, "de", translationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Translation not found");
    }

    // ========================================================================
    // Delete Translation Tests
    // ========================================================================

    @Test
    void deleteTranslation_ShouldDeleteSuccessfully() {
        // Given
        FormTranslation translation = FormTranslation.builder()
                .id(UUID.randomUUID())
                .formDefinition(testForm)
                .language("de")
                .translations(testTranslations)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.findByFormDefinitionAndLanguage(testForm, "de"))
                .thenReturn(Optional.of(translation));

        // When
        formTranslationService.deleteTranslation(formId, "de");

        // Then
        verify(formTranslationRepository, times(1)).delete(translation);
    }

    @Test
    void deleteTranslation_TranslationNotFound_ShouldThrowException() {
        // Given
        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.findByFormDefinitionAndLanguage(testForm, "de"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> formTranslationService.deleteTranslation(formId, "de"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Translation not found");
    }

    // ========================================================================
    // Get Translation Tests
    // ========================================================================

    @Test
    void getTranslation_ShouldReturnTranslation() {
        // Given
        FormTranslation translation = FormTranslation.builder()
                .id(UUID.randomUUID())
                .formDefinition(testForm)
                .language("de")
                .translations(testTranslations)
                .build();

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.findByFormDefinitionAndLanguage(testForm, "de"))
                .thenReturn(Optional.of(translation));

        // When
        FormTranslationDTO result = formTranslationService.getTranslation(formId, "de");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLanguage()).isEqualTo("de");
    }

    @Test
    void getTranslationsByForm_ShouldReturnAllTranslations() {
        // Given
        List<FormTranslation> translations = List.of(
                FormTranslation.builder()
                        .id(UUID.randomUUID())
                        .formDefinition(testForm)
                        .language("de")
                        .translations(testTranslations)
                        .build(),
                FormTranslation.builder()
                        .id(UUID.randomUUID())
                        .formDefinition(testForm)
                        .language("en")
                        .translations(testTranslations)
                        .build()
        );

        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.findByFormDefinition(testForm)).thenReturn(translations);

        // When
        List<FormTranslationDTO> result = formTranslationService.getTranslationsByForm(formId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(FormTranslationDTO::getLanguage).containsExactlyInAnyOrder("de", "en");
    }

    @Test
    void getTranslationsByLanguage_ShouldReturnTranslationsForLanguage() {
        // Given
        List<FormTranslation> germanTranslations = List.of(
                FormTranslation.builder()
                        .id(UUID.randomUUID())
                        .formDefinition(testForm)
                        .language("de")
                        .translations(testTranslations)
                        .build()
        );

        when(formTranslationRepository.findByLanguage("de")).thenReturn(germanTranslations);

        // When
        List<FormTranslationDTO> result = formTranslationService.getTranslationsByLanguage("de");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getLanguage()).isEqualTo("de");
    }

    // ========================================================================
    // Utility Tests
    // ========================================================================

    @Test
    void translationExists_ShouldReturnTrue() {
        // Given
        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.existsByFormDefinitionAndLanguage(testForm, "de")).thenReturn(true);

        // When
        boolean result = formTranslationService.translationExists(formId, "de");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void translationExists_ShouldReturnFalse() {
        // Given
        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.existsByFormDefinitionAndLanguage(testForm, "de")).thenReturn(false);

        // When
        boolean result = formTranslationService.translationExists(formId, "de");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void countTranslations_ShouldReturnCount() {
        // Given
        when(formDefinitionRepository.findById(formId)).thenReturn(Optional.of(testForm));
        when(formTranslationRepository.countByFormDefinition(testForm)).thenReturn(3L);

        // When
        long result = formTranslationService.countTranslations(formId);

        // Then
        assertThat(result).isEqualTo(3L);
    }
}
