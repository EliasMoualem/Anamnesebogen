package de.elias.moualem.Anamnesebogen.controller.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionCreateDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormDefinitionUpdateDTO;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormCategory;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition.FormStatus;
import de.elias.moualem.Anamnesebogen.service.forms.FormDefinitionService;
import de.elias.moualem.Anamnesebogen.service.forms.FormTranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FormBuilderController.
 */
@WebMvcTest(FormBuilderController.class)
class FormBuilderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FormDefinitionService formDefinitionService;

    @MockBean
    private FormTranslationService formTranslationService;

    private ObjectNode testSchema;
    private ObjectNode testUiSchema;
    private FormDefinitionDTO testFormDTO;
    private UUID testFormId;

    @BeforeEach
    void setUp() {
        testFormId = UUID.randomUUID();
        testSchema = objectMapper.createObjectNode();
        testSchema.put("type", "object");
        testUiSchema = objectMapper.createObjectNode();
        testUiSchema.put("title", "Test Form");

        testFormDTO = FormDefinitionDTO.builder()
                .id(testFormId)
                .name("Test Form")
                .description("Test Description")
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .status(FormStatus.DRAFT)
                .isActive(false)
                .isDefault(false)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();
    }

    // ========================================================================
    // GET Endpoint Tests
    // ========================================================================

    @Test
    void getAllForms_ShouldReturnAllForms() throws Exception {
        // Given
        List<FormDefinitionDTO> forms = List.of(testFormDTO);
        when(formDefinitionService.getAllFormDefinitions()).thenReturn(forms);

        // When / Then
        mockMvc.perform(get("/api/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Form")));
    }

    @Test
    void getAllForms_WithCategoryFilter_ShouldReturnFilteredForms() throws Exception {
        // Given
        List<FormDefinitionDTO> forms = List.of(testFormDTO);
        when(formDefinitionService.getFormsByCategory(FormCategory.ANAMNESIS)).thenReturn(forms);

        // When / Then
        mockMvc.perform(get("/api/forms")
                        .param("category", "ANAMNESIS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category", is("ANAMNESIS")));
    }

    @Test
    void getFormById_ShouldReturnForm() throws Exception {
        // Given
        when(formDefinitionService.getFormDefinitionById(testFormId)).thenReturn(testFormDTO);

        // When / Then
        mockMvc.perform(get("/api/forms/{id}", testFormId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testFormId.toString())))
                .andExpect(jsonPath("$.name", is("Test Form")));
    }

    @Test
    void getFormById_NotFound_ShouldReturn404() throws Exception {
        // Given
        when(formDefinitionService.getFormDefinitionById(any(UUID.class)))
                .thenThrow(new IllegalArgumentException("Form not found"));

        // When / Then
        mockMvc.perform(get("/api/forms/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActiveForm_ShouldReturnActiveForm() throws Exception {
        // Given
        testFormDTO.setStatus(FormStatus.PUBLISHED);
        testFormDTO.setIsActive(true);
        when(formDefinitionService.getActiveFormByCategory(FormCategory.ANAMNESIS))
                .thenReturn(Optional.of(testFormDTO));

        // When / Then
        mockMvc.perform(get("/api/forms/active")
                        .param("category", "ANAMNESIS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Form")))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    void getActiveForm_NotFound_ShouldReturn404() throws Exception {
        // Given
        when(formDefinitionService.getActiveFormByCategory(FormCategory.ANAMNESIS))
                .thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/forms/active")
                        .param("category", "ANAMNESIS"))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // POST Create Endpoint Tests
    // ========================================================================

    @Test
    void createForm_ShouldCreateAndReturn201() throws Exception {
        // Given
        FormDefinitionCreateDTO createDTO = FormDefinitionCreateDTO.builder()
                .name("New Form")
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionService.createFormDefinition(any(FormDefinitionCreateDTO.class)))
                .thenReturn(testFormDTO);

        // When / Then
        mockMvc.perform(post("/api/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Form")));
    }

    @Test
    void createForm_InvalidData_ShouldReturn400() throws Exception {
        // Given - empty name (validation should fail)
        FormDefinitionCreateDTO createDTO = FormDefinitionCreateDTO.builder()
                .name("")  // Invalid - blank name
                .category(FormCategory.ANAMNESIS)
                .version("1.0.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        // When / Then
        mockMvc.perform(post("/api/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // PUT Update Endpoint Tests
    // ========================================================================

    @Test
    void updateForm_ShouldUpdateAndReturn200() throws Exception {
        // Given
        FormDefinitionUpdateDTO updateDTO = FormDefinitionUpdateDTO.builder()
                .name("Updated Form")
                .version("1.1.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        FormDefinitionDTO updatedForm = FormDefinitionDTO.builder()
                .id(testFormId)
                .name("Updated Form")
                .version("1.1.0")
                .category(FormCategory.ANAMNESIS)
                .status(FormStatus.DRAFT)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionService.updateFormDefinition(eq(testFormId), any(FormDefinitionUpdateDTO.class)))
                .thenReturn(updatedForm);

        // When / Then
        mockMvc.perform(put("/api/forms/{id}", testFormId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Form")))
                .andExpect(jsonPath("$.version", is("1.1.0")));
    }

    @Test
    void updateForm_NotFound_ShouldReturn404() throws Exception {
        // Given
        FormDefinitionUpdateDTO updateDTO = FormDefinitionUpdateDTO.builder()
                .name("Updated Form")
                .version("1.1.0")
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionService.updateFormDefinition(any(UUID.class), any(FormDefinitionUpdateDTO.class)))
                .thenThrow(new IllegalArgumentException("Form not found"));

        // When / Then
        mockMvc.perform(put("/api/forms/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // DELETE Endpoint Tests
    // ========================================================================

    @Test
    void deleteForm_ShouldDeleteAndReturn204() throws Exception {
        // Given
        doNothing().when(formDefinitionService).deleteFormDefinition(testFormId);

        // When / Then
        mockMvc.perform(delete("/api/forms/{id}", testFormId))
                .andExpect(status().isNoContent());

        verify(formDefinitionService, times(1)).deleteFormDefinition(testFormId);
    }

    @Test
    void deleteForm_NotFound_ShouldReturn404() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Form not found"))
                .when(formDefinitionService).deleteFormDefinition(any(UUID.class));

        // When / Then
        mockMvc.perform(delete("/api/forms/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // Lifecycle Endpoint Tests
    // ========================================================================

    @Test
    void publishForm_ShouldPublishAndReturn200() throws Exception {
        // Given
        FormDefinitionDTO publishedForm = FormDefinitionDTO.builder()
                .id(testFormId)
                .name("Test Form")
                .category(FormCategory.ANAMNESIS)
                .status(FormStatus.PUBLISHED)
                .isActive(true)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionService.publishFormDefinition(eq(testFormId), anyString(), eq(true)))
                .thenReturn(publishedForm);

        // When / Then
        mockMvc.perform(post("/api/forms/{id}/publish", testFormId)
                        .param("setActive", "true")
                        .param("publishedBy", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    void archiveForm_ShouldArchiveAndReturn200() throws Exception {
        // Given
        FormDefinitionDTO archivedForm = FormDefinitionDTO.builder()
                .id(testFormId)
                .name("Test Form")
                .status(FormStatus.ARCHIVED)
                .isActive(false)
                .schema(testSchema)
                .uiSchema(testUiSchema)
                .build();

        when(formDefinitionService.archiveFormDefinition(testFormId)).thenReturn(archivedForm);

        // When / Then
        mockMvc.perform(post("/api/forms/{id}/archive", testFormId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")))
                .andExpect(jsonPath("$.isActive", is(false)));
    }

    // ========================================================================
    // Translation Endpoint Tests
    // ========================================================================

    @Test
    void getTranslations_ShouldReturnAllTranslations() throws Exception {
        // Given
        ObjectNode translations = objectMapper.createObjectNode();
        translations.put("firstName", "Vorname");

        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .id(UUID.randomUUID())
                .language("de")
                .translations(translations)
                .build();

        when(formTranslationService.getTranslationsByForm(testFormId))
                .thenReturn(List.of(translationDTO));

        // When / Then
        mockMvc.perform(get("/api/forms/{id}/translations", testFormId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].language", is("de")));
    }

    @Test
    void addTranslation_ShouldCreateAndReturn201() throws Exception {
        // Given
        ObjectNode translations = objectMapper.createObjectNode();
        translations.put("firstName", "Vorname");

        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("de")
                .translations(translations)
                .build();

        FormTranslationDTO createdTranslation = FormTranslationDTO.builder()
                .id(UUID.randomUUID())
                .language("de")
                .translations(translations)
                .build();

        when(formTranslationService.addTranslation(eq(testFormId), any(FormTranslationDTO.class)))
                .thenReturn(createdTranslation);

        // When / Then
        mockMvc.perform(post("/api/forms/{id}/translations", testFormId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(translationDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language", is("de")));
    }

    @Test
    void updateTranslation_ShouldUpdateAndReturn200() throws Exception {
        // Given
        ObjectNode translations = objectMapper.createObjectNode();
        translations.put("firstName", "Vorname (aktualisiert)");

        FormTranslationDTO translationDTO = FormTranslationDTO.builder()
                .language("de")
                .translations(translations)
                .build();

        FormTranslationDTO updatedTranslation = FormTranslationDTO.builder()
                .id(UUID.randomUUID())
                .language("de")
                .translations(translations)
                .build();

        when(formTranslationService.updateTranslation(eq(testFormId), eq("de"), any(FormTranslationDTO.class)))
                .thenReturn(updatedTranslation);

        // When / Then
        mockMvc.perform(put("/api/forms/{id}/translations/{language}", testFormId, "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(translationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language", is("de")));
    }

    @Test
    void deleteTranslation_ShouldDeleteAndReturn204() throws Exception {
        // Given
        doNothing().when(formTranslationService).deleteTranslation(testFormId, "de");

        // When / Then
        mockMvc.perform(delete("/api/forms/{id}/translations/{language}", testFormId, "de"))
                .andExpect(status().isNoContent());

        verify(formTranslationService, times(1)).deleteTranslation(testFormId, "de");
    }
}
