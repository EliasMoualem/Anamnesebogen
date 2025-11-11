package de.elias.moualem.Anamnesebogen.service;

import de.elias.moualem.Anamnesebogen.dto.PatientFormDTO;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletOutputStream outputStream;

    private PdfService pdfService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        pdfService = new PdfService(templateEngine);
    }

    @Test
    void generatePdf_shouldCreatePdfFile() throws Exception {
        // Given
        PatientFormDTO patient = createTestPatient();
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);

        // When
        File result = pdfService.generatePdf(patient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.getName()).contains("anamnese_");
        assertThat(result.getName()).endsWith(".pdf");
        assertThat(result.length()).isGreaterThan(0);

        // Verify template processing
        verify(templateEngine).process(eq("pdf_anamnesebogen"), any(Context.class));

        // Cleanup
        result.delete();
    }

    @Test
    void generatePdf_shouldProcessPatientData() throws Exception {
        // Given
        PatientFormDTO patient = createTestPatient();
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);

        // When
        File result = pdfService.generatePdf(patient);

        // Then
        verify(templateEngine).process(eq("pdf_anamnesebogen"), argThat(context -> {
            Context ctx = (Context) context;
            return ctx.getVariable("patientForm") != null;
        }));

        // Cleanup
        result.delete();
    }

    @Test
    void generatePdf_shouldIncludeSignatureWhenPresent() throws Exception {
        // Given
        PatientFormDTO patient = createTestPatient();
        byte[] signature = "test signature data".getBytes();
        patient.setSignature(signature);
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);

        // When
        File result = pdfService.generatePdf(patient);

        // Then
        verify(templateEngine).process(eq("pdf_anamnesebogen"), argThat(context -> {
            Context ctx = (Context) context;
            return ctx.getVariable("signatureImage") != null;
        }));

        // Cleanup
        result.delete();
    }

    @Test
    void generatePdf_shouldIncludeSignatureMetadataWhenPresent() throws Exception {
        // Given
        PatientFormDTO patient = createTestPatient();
        byte[] signature = "test signature data".getBytes();
        UUID signatureId = UUID.randomUUID();
        LocalDateTime signatureTimestamp = LocalDateTime.now();

        patient.setSignature(signature);
        patient.setSignatureId(signatureId);
        patient.setSignatureTimestamp(signatureTimestamp);

        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);

        // When
        File result = pdfService.generatePdf(patient);

        // Then
        verify(templateEngine).process(eq("pdf_anamnesebogen"), argThat(context -> {
            Context ctx = (Context) context;
            Object signatureIdVar = ctx.getVariable("signatureId");
            Object signatureTimestampVar = ctx.getVariable("signatureTimestamp");

            // Verify that signature metadata is included in context
            return signatureIdVar != null
                    && signatureTimestampVar != null
                    && signatureIdVar.equals(signatureId)
                    && signatureTimestampVar.equals(signatureTimestamp);
        }));

        // Cleanup
        result.delete();
    }

    @Test
    void generatePdf_shouldHandleMissingSignature() throws Exception {
        // Given
        PatientFormDTO patient = createTestPatient();
        patient.setSignature(null);
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);

        // When
        File result = pdfService.generatePdf(patient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();

        // Cleanup
        result.delete();
    }

    @Test
    void generateAndServePdf_shouldWritePdfToResponse() throws Exception {
        // Given
        PatientFormDTO patient = createTestPatient();
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);
        when(response.getOutputStream()).thenReturn(outputStream);

        // When
        pdfService.generateAndServePdf(patient, response);

        // Then
        verify(response).setContentType("application/pdf");
        verify(response).addHeader(eq("Content-Disposition"),
                contains("Anamnesebogen_" + patient.getFirstName() + ".pdf"));
        verify(response, times(2)).getOutputStream(); // Called twice: once for copy, once for flush
        verify(outputStream).flush();
    }

    @Test
    void generateAndServePdf_shouldUseDefaultFilenameWhenFirstNameIsNull() throws Exception {
        // Given
        PatientFormDTO patient = createTestPatient();
        patient.setFirstName(null);
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);
        when(response.getOutputStream()).thenReturn(outputStream);

        // When
        pdfService.generateAndServePdf(patient, response);

        // Then
        verify(response).addHeader(eq("Content-Disposition"),
                contains("Anamnesebogen_patient.pdf"));
    }

    @Test
    void generateAndServePdf_shouldThrowExceptionWhenPdfGenerationFails() {
        // Given
        PatientFormDTO patient = createTestPatient();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // When/Then
        assertThatThrownBy(() -> pdfService.generateAndServePdf(patient, response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Template processing failed");
    }

    @Test
    void generatePdf_shouldHandlePatientWithAllFields() throws Exception {
        // Given
        PatientFormDTO patient = createFullPatient();
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);

        // When
        File result = pdfService.generatePdf(patient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.length()).isGreaterThan(0);

        // Cleanup
        result.delete();
    }

    @Test
    void generatePdf_shouldHandlePatientWithMinimalFields() throws Exception {
        // Given
        PatientFormDTO patient = new PatientFormDTO();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setBirthDate("2010-01-01");
        String mockHtml = createMockHtmlContent();

        when(templateEngine.process(eq("pdf_anamnesebogen"), any(Context.class)))
                .thenReturn(mockHtml);

        // When
        File result = pdfService.generatePdf(patient);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();

        // Cleanup
        result.delete();
    }

    // Helper methods

    private PatientFormDTO createTestPatient() {
        PatientFormDTO patient = new PatientFormDTO();
        patient.setFirstName("Max");
        patient.setLastName("Mustermann");
        patient.setBirthDate("2010-05-15");
        patient.setGender("male");
        patient.setStreet("Musterstraße 123");
        patient.setZipCode("12345");
        patient.setCity("Berlin");
        patient.setMobileNumber("0171-1234567");
        patient.setEmailAddress("max.mustermann@example.com");
        return patient;
    }

    private PatientFormDTO createFullPatient() {
        PatientFormDTO patient = createTestPatient();
        patient.setPhoneNumber("030-12345678");
        patient.setLanguage("de");
        patient.setInsuranceProvider("AOK");
        patient.setInsurancePolicyNumber("123456789");
        patient.setInsuranceGroupNumber("GRP001");
        patient.setPolicyholderName("Parent Mustermann");
        patient.setRelationshipToPolicyholder("Parent");
        patient.setAllergies("Pollen");
        patient.setCurrentMedications("Aspirin");
        patient.setMedicalConditions("Asthma");
        patient.setPreviousSurgeries("None");
        patient.setPrimaryCareDoctor("Dr. Schmidt");
        patient.setSignature("signature data".getBytes());
        return patient;
    }

    private String createMockHtmlContent() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Anamnesebogen</title>
                    <style>
                        body { font-family: Arial, sans-serif; }
                    </style>
                </head>
                <body>
                    <h1>Anamnesebogen</h1>
                    <p>Patient Information</p>
                </body>
                </html>
                """;
    }
}
