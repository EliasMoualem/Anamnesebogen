package de.elias.moualem.Anamnesebogen.controller;

import com.lowagie.text.DocumentException;
import de.elias.moualem.Anamnesebogen.model.MinorPatient;
import de.elias.moualem.Anamnesebogen.service.PdfService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling patient anamnesis form operations.
 * Manages form display, form submission, and PDF generation.
 */
@Controller
@Slf4j
@Timed
@RequiredArgsConstructor
public class AnamnesebogenController {

    private final PdfService pdfService;

    /**
     * In-memory storage for temporary patient data.
     * Data is removed after PDF generation.
     */
    private final Map<String, MinorPatient> patientStore = new HashMap<>();

    /**
     * Displays the language selection page.
     *
     * @return View name for the language selection
     */
    @GetMapping("/")
    public String languageSelection() {
        return "language-selection";
    }

    /**
     * Displays the anamnesis form in the selected language.
     *
     * @param model Model to add attributes to for view rendering
     * @param error Optional error code from previous operations
     * @param lang Language code (de, en, ar, ru)
     * @return View name for the anamnesis form
     */
    @GetMapping("/anamnesebogen")
    public String anamnesebogen(
            Model model,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "lang", defaultValue = "de") String lang) {

        // Add a new patient to the model
        model.addAttribute("minorPatient", new MinorPatient());

        // Add the selected language to the model
        model.addAttribute("lang", lang);

        // Add specific error message based on error code
        if (error != null) {
            model.addAttribute("errorMessage", getErrorMessage(error, lang));
        }

        // Return the appropriate language template
        return "anamnesebogen-" + lang;
    }

    /**
     * Processes the anamnesis form submission.
     *
     * @param minorPatient Patient data from the form
     * @param signatureData Optional signature data as base64 string
     * @param lang Language code used for the form
     * @return Redirect to PDF generation
     */
    @PostMapping("/submit-anamnesebogen")
    public String submitAnamnesebogen(
            @ModelAttribute MinorPatient minorPatient,
            @RequestParam(value = "signatureData", required = false) String signatureData,
            @RequestParam(value = "lang", defaultValue = "de") String lang) {

        try {
            log.info("Processing form submission for patient: {} {} in language: {}",
                    minorPatient.getFirstName(), minorPatient.getLastName(), lang);

            // Store the language with the patient data
            minorPatient.setLanguage(lang);

            // Process signature if available
            processSignature(minorPatient, signatureData);

            // Store patient data with unique ID
            String patientId = storePatientData(minorPatient);

            // Redirect to PDF generation
            return "redirect:/pdf?id=" + patientId;

        } catch (Exception e) {
            log.error("Error processing form submission", e);
            return "redirect:/anamnesebogen?error=submission_error&lang=" + lang;
        }
    }

    /**
     * Generates and serves PDF file for the patient data.
     *
     * @param patientId ID of the patient data in the store
     * @param response HTTP response to write PDF to
     */
    @GetMapping("/pdf")
    public void createAndStorePDF(
            @RequestParam(value = "id", required = false) String patientId,
            HttpServletResponse response) {

        log.info("PDF endpoint called with ID: {}", patientId);
        String lang = "de"; // Default language

        try {
            // Validate patient ID
            if (patientId == null || patientId.isEmpty()) {
                handleMissingPatientId(response, lang);
                return;
            }

            // Get patient data
            MinorPatient minorPatient = patientStore.get(patientId);
            if (minorPatient == null) {
                handlePatientNotFound(response, patientId, lang);
                return;
            }

            // Get the language from patient data
            lang = minorPatient.getLanguage() != null ? minorPatient.getLanguage() : "de";

            log.info("Found patient: {} {} with language: {}",
                    minorPatient.getFirstName(), minorPatient.getLastName(), lang);
            log.info("Has signature: {}", minorPatient.hasSignature());

            // Delegate PDF generation and serving to PdfService
            try {
                pdfService.generateAndServePdf(minorPatient, response);

                // Clean up after successfully serving the PDF
                patientStore.remove(patientId);
                log.info("PDF served successfully, patient data removed from store");
            } catch (DocumentException e) {
                log.error("Error generating PDF document", e);
                response.sendRedirect("/anamnesebogen?error=pdf_error&lang=" + lang);
            } catch (IOException e) {
                log.error("I/O error when serving PDF", e);
                response.sendRedirect("/anamnesebogen?error=pdf_io_error&lang=" + lang);
            } catch (Exception e) {
                // Re-throw to outer handler to treat as unexpected
                throw e;
            }

        } catch (Exception e) {
            handleUnexpectedError(response, e, lang);
        }
    }

    /**
     * Global exception handler for this controller.
     *
     * @param e Exception that was thrown
     * @param response HTTP response to redirect
     */
    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) {
        log.error("Unhandled exception in AnamnesebogenController", e);

        try {
            // Determine error type
            String errorCode = determineErrorCode(e);
            // Default to German for unknown languages
            response.sendRedirect("/anamnesebogen?error=" + errorCode + "&lang=de");
        } catch (IOException ex) {
            log.error("Failed to redirect after exception", ex);
        }
    }

    // ===============================
    // = Helper Methods             =
    // ===============================

    /**
     * Maps error codes to human-readable error messages based on language.
     *
     * @param error Error code
     * @param lang Language code
     * @return Human-readable error message
     */
    private String getErrorMessage(String error, String lang) {
        // Error messages for German (default)
        if ("de".equals(lang)) {
            return switch (error) {
                case "no_data" -> "Patientendaten nicht gefunden. Bitte versuchen Sie es erneut.";
                case "pdf_error" -> "Fehler beim Generieren des PDFs. Bitte versuchen Sie es erneut.";
                case "missing_id" -> "Fehlende Patienten-ID. Bitte versuchen Sie es erneut.";
                case "pdf_not_found" -> "Generierte PDF-Datei nicht gefunden. Bitte versuchen Sie es erneut.";
                case "pdf_io_error" -> "Fehler beim Schreiben der PDF-Datei. Bitte versuchen Sie es erneut.";
                case "submission_error" -> "Fehler bei der Verarbeitung Ihres Formulars. Bitte versuchen Sie es erneut.";
                case "unexpected" -> "Ein unerwarteter Fehler ist aufgetreten. Bitte versuchen Sie es erneut.";
                default -> "Ein Fehler ist aufgetreten: " + error + ". Bitte versuchen Sie es erneut.";
            };
        }
        // Error messages for English
        else if ("en".equals(lang)) {
            return switch (error) {
                case "no_data" -> "Patient data not found. Please try again.";
                case "pdf_error" -> "Error generating PDF. Please try again.";
                case "missing_id" -> "Missing patient ID. Please try again.";
                case "pdf_not_found" -> "Generated PDF file not found. Please try again.";
                case "pdf_io_error" -> "Error writing PDF file. Please try again.";
                case "submission_error" -> "Error processing your form. Please try again.";
                case "unexpected" -> "An unexpected error occurred. Please try again.";
                default -> "An error occurred: " + error + ". Please try again.";
            };
        }
        // Error messages for Arabic
        else if ("ar".equals(lang)) {
            return switch (error) {
                case "no_data" -> "لم يتم العثور على بيانات المريض. يرجى المحاولة مرة أخرى.";
                case "pdf_error" -> "خطأ في إنشاء ملف PDF. يرجى المحاولة مرة أخرى.";
                case "missing_id" -> "معرف المريض مفقود. يرجى المحاولة مرة أخرى.";
                case "pdf_not_found" -> "لم يتم العثور على ملف PDF الذي تم إنشاؤه. يرجى المحاولة مرة أخرى.";
                case "pdf_io_error" -> "خطأ في كتابة ملف PDF. يرجى المحاولة مرة أخرى.";
                case "submission_error" -> "خطأ في معالجة النموذج الخاص بك. يرجى المحاولة مرة أخرى.";
                case "unexpected" -> "حدث خطأ غير متوقع. يرجى المحاولة مرة أخرى.";
                default -> "حدث خطأ: " + error + ". يرجى المحاولة مرة أخرى.";
            };
        }
        // Error messages for Russian
        else if ("ru".equals(lang)) {
            return switch (error) {
                case "no_data" -> "Данные пациента не найдены. Пожалуйста, попробуйте еще раз.";
                case "pdf_error" -> "Ошибка при создании PDF. Пожалуйста, попробуйте еще раз.";
                case "missing_id" -> "Отсутствует ID пациента. Пожалуйста, попробуйте еще раз.";
                case "pdf_not_found" -> "Созданный PDF-файл не найден. Пожалуйста, попробуйте еще раз.";
                case "pdf_io_error" -> "Ошибка при записи PDF-файла. Пожалуйста, попробуйте еще раз.";
                case "submission_error" -> "Ошибка при обработке вашей формы. Пожалуйста, попробуйте еще раз.";
                case "unexpected" -> "Произошла непредвиденная ошибка. Пожалуйста, попробуйте еще раз.";
                default -> "Произошла ошибка: " + error + ". Пожалуйста, попробуйте еще раз.";
            };
        }
        // Default to German for unknown languages
        else {
            return getErrorMessage(error, "de");
        }
    }

    /**
     * Processes and stores signature data from form submission.
     *
     * @param minorPatient Patient to attach signature to
     * @param signatureData Base64 encoded signature data
     */
    private void processSignature(MinorPatient minorPatient, String signatureData) {
        if (signatureData != null && !signatureData.isEmpty()) {
            log.info("Processing signature data of length: {}", signatureData.length());

            try {
                // Extract base64 data from the data URL
                String[] parts = signatureData.split(",");
                if (parts.length > 1) {
                    String base64Data = parts[1];
                    byte[] signatureBytes = Base64.getDecoder().decode(base64Data);
                    minorPatient.setSignature(signatureBytes);
                    log.info("Signature processed successfully, byte length: {}", signatureBytes.length);
                } else {
                    log.warn("Invalid signature format, no comma found in data URL");
                }
            } catch (IllegalArgumentException e) {
                log.error("Error decoding signature data", e);
                // Continue without signature rather than failing the whole form
            }
        } else {
            log.info("No signature data received");
        }
    }

    /**
     * Stores patient data with a UUID.
     *
     * @param minorPatient Patient data to store
     * @return Generated patient ID
     */
    private String storePatientData(MinorPatient minorPatient) {
        String patientId = UUID.randomUUID().toString();
        patientStore.put(patientId, minorPatient);
        log.info("Patient stored with ID: {}", patientId);
        return patientId;
    }

    /**
     * Handles case when patient ID is missing.
     *
     * @param response HTTP response for redirect
     * @param lang Language code
     * @throws IOException If redirect fails
     */
    private void handleMissingPatientId(HttpServletResponse response, String lang) throws IOException {
        log.error("No patient ID provided to /pdf endpoint");
        response.sendRedirect("/anamnesebogen?error=missing_id&lang=" + lang);
    }

    /**
     * Handles case when patient data is not found.
     *
     * @param response HTTP response for redirect
     * @param patientId ID that was not found
     * @param lang Language code
     * @throws IOException If redirect fails
     */
    private void handlePatientNotFound(HttpServletResponse response, String patientId, String lang) throws IOException {
        log.error("Patient data not found for ID: {}", patientId);
        response.sendRedirect("/anamnesebogen?error=no_data&lang=" + lang);
    }

    /**
     * Handles unexpected errors in PDF endpoint.
     *
     * @param response HTTP response for redirect
     * @param e Exception that occurred
     * @param lang Language code
     */
    private void handleUnexpectedError(HttpServletResponse response, Exception e, String lang) {
        log.error("Unexpected error in PDF endpoint", e);
        try {
            response.sendRedirect("/anamnesebogen?error=unexpected&lang=" + lang);
        } catch (IOException ex) {
            log.error("Failed to redirect after error", ex);
        }
    }

    /**
     * Determines error code based on exception type.
     *
     * @param e Exception to analyze
     * @return Appropriate error code
     */
    private String determineErrorCode(Exception e) {
        if (e instanceof DocumentException) {
            return "pdf_error";
        } else if (e instanceof IOException) {
            return "pdf_io_error";
        }
        return "unexpected";
    }
}

