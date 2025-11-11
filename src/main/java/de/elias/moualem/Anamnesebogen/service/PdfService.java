package de.elias.moualem.Anamnesebogen.service;

import de.elias.moualem.Anamnesebogen.dto.PatientFormDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Base64;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.servlet.http.HttpServletResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    /**
     * Format gender string for display
     * This is now a static utility method that can be called from Thymeleaf
     */
    public static String formatGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) return "-";

        return switch(gender) {
            case "male" -> "Männlich";
            case "female" -> "Weiblich";
            case "diverse" -> "Divers";
            default -> gender;
        };
    }

    /**
     * Generate PDF file for a patient's anamnesis data.
     *
     * @param patient The patient data to generate PDF for
     * @return File object of the generated PDF
     * @throws Exception If PDF generation fails
     */
    public File generatePdf(PatientFormDTO patient) throws Exception {
        // Create temporary file
        Path tempFile = Files.createTempFile("anamnese_", ".pdf");
        File pdfFile = tempFile.toFile();

        // Process HTML template
        String processedHtml = processTemplate(patient);

        // Convert HTML to PDF
        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(processedHtml);
            renderer.layout();
            renderer.createPDF(outputStream);
        }

        log.info("PDF generated: {}", pdfFile.getAbsolutePath());
        return pdfFile;
    }

    /**
     * Generate PDF for patient and write it to the provided HttpServletResponse.
     * This method does not perform any application-level redirects; it throws
     * exceptions which the caller (controller) should handle and translate into
     * user-facing redirects if needed.
     *
     * @param minorPatient patient data used to generate the PDF
     * @param response HTTP response to write the resulting PDF to
     * @throws Exception when PDF creation or IO fails
     */
    public void generateAndServePdf(PatientFormDTO minorPatient, HttpServletResponse response) throws Exception {
        // Use existing generatePdf implementation
        File pdfFile = generatePdf(minorPatient);
        Path file = pdfFile.toPath();

        if (Files.exists(file)) {
            // Set response headers
            response.setContentType("application/pdf");
            response.addHeader("Content-Disposition",
                    "attachment; filename=Anamnesebogen_" + (minorPatient.getFirstName() != null ? minorPatient.getFirstName() : "patient") + ".pdf");

            // Copy file to response output stream
            Files.copy(file, response.getOutputStream());
            response.getOutputStream().flush();

            log.info("PDF written to response: {}", file);
        } else {
            log.error("Generated PDF file does not exist: {}", file);
            throw new java.io.IOException("Generated PDF file not found: " + file);
        }
    }

    /**
     * Process Thymeleaf template with patient data
     *
     * @param patient The patient data to use in template
     * @return Processed HTML string
     */
    private String processTemplate(PatientFormDTO patient) {
        // Create Thymeleaf context and add patient data
        Context context = new Context();
        context.setVariable("patientForm", patient);

        // Load and embed logo image
        try {
            byte[] logoBytes = getClass().getClassLoader()
                    .getResourceAsStream("templates/logo.PNG")
                    .readAllBytes();
            String base64Logo = Base64.getEncoder().encodeToString(logoBytes);
            context.setVariable("logoImage", base64Logo);
            log.debug("Logo image loaded and added to PDF context");
        } catch (Exception e) {
            log.warn("Could not load logo image for PDF", e);
            // Logo is optional, continue without it
        }

        // If signature exists, convert it to base64 for embedding in HTML
        if (patient.hasSignature()) {
            try {
                String base64Signature = Base64.getEncoder().encodeToString(patient.getSignature());
                context.setVariable("signatureImage", base64Signature);

                // Add signature metadata for verification display
                context.setVariable("signatureId", patient.getSignatureId());
                context.setVariable("signatureTimestamp", patient.getSignatureTimestamp());

                log.debug("Signature metadata added to PDF context: ID={}, Timestamp={}",
                    patient.getSignatureId(), patient.getSignatureTimestamp());
            } catch (Exception e) {
                log.error("Error processing signature for PDF", e);
            }
        }

        // Process template with context
        return templateEngine.process("pdf_anamnesebogen", context);
    }
}