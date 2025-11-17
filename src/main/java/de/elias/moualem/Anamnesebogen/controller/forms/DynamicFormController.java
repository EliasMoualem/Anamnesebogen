package de.elias.moualem.Anamnesebogen.controller.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.elias.moualem.Anamnesebogen.dto.forms.FormTranslationDTO;
import de.elias.moualem.Anamnesebogen.entity.FormDefinition;
import de.elias.moualem.Anamnesebogen.entity.FormSubmission;
import de.elias.moualem.Anamnesebogen.service.forms.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for rendering and submitting dynamic forms generated from FormDefinition entities.
 * Handles patient-facing form display, submission, and PDF generation.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DynamicFormController {

    private final FormDefinitionService formDefinitionService;
    private final FormTranslationService translationService;
    private final ThymeleafTemplateGenerator templateGenerator;
    private final DynamicFormPdfService pdfService;
    private final FormSubmissionService submissionService;
    private final FormValidationService validationService;
    private final ObjectMapper objectMapper;

    /**
     * Displays a dynamic form to the patient.
     * GET /forms/{id}?lang=de
     *
     * @param formId   UUID of the form definition
     * @param language Language code (de, en, ar, ru)
     * @param model    Spring MVC model
     * @return Template name
     */
    @GetMapping("/forms/{id}")
    public String showForm(@PathVariable("id") UUID formId,
                           @RequestParam(value = "lang", defaultValue = "de") String language,
                           @RequestParam(value = "error", required = false) String error,
                           Model model,
                           HttpServletRequest request) {

        log.info("Displaying form id={}, language={}, IP={}",
                formId, language, request.getRemoteAddr());

        try {
            // Load form definition
            FormDefinition formDefinition = formDefinitionService.getFormById(formId);

            // Check if form is published and active
            if (formDefinition.getStatus() != FormDefinition.FormStatus.PUBLISHED) {
                log.warn("Attempted to access unpublished form: {}", formId);
                model.addAttribute("error", true);
                model.addAttribute("errorMessage", "This form is not available.");
                return "error/form-not-found";
            }

            // Load translation if available
            FormTranslationDTO translation = null;
            try {
                translation = translationService.getTranslation(formId, language);
            } catch (Exception e) {
                log.warn("Translation not found for form {} in language {}, using defaults",
                        formId, language);
            }

            // Prepare form data (empty map for initial display)
            Map<String, Object> formData = new HashMap<>();

            // Generate dynamic field HTML
            String dynamicFieldsHtml = templateGenerator.generateFieldsOnly(
                    formDefinition,
                    language,
                    translation
            );

            // Add model attributes
            model.addAttribute("formDefinition", formDefinition);
            model.addAttribute("formData", formData);
            model.addAttribute("formDataMap", formData);  // Pass Map directly for Thymeleaf JavaScript inlining
            model.addAttribute("validationErrorsMap", Collections.emptyMap());  // Empty map for initial page load
            model.addAttribute("dynamicFieldsHtml", dynamicFieldsHtml);
            model.addAttribute("language", language);
            model.addAttribute("isRtl", "ar".equalsIgnoreCase(language));
            model.addAttribute("error", error != null);
            model.addAttribute("errorMessage", getErrorMessage(error, language));

            // Return base template (dynamic fields will be injected)
            return "base/dynamic-form-layout";

        } catch (Exception e) {
            log.error("Failed to display form {}", formId, e);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "An error occurred while loading the form.");
            return "error/form-not-found";
        }
    }

    /**
     * Handles form submission.
     * POST /submit-form/{id}
     *
     * @param formId  UUID of the form definition
     * @param request HTTP request containing form data
     * @return Redirect to success page or back to form with errors
     */
    @PostMapping("/submit-form/{id}")
    public String submitForm(@PathVariable("id") UUID formId,
                             @RequestParam(value = "lang", defaultValue = "de") String language,
                             HttpServletRequest request,
                             Model model) {

        log.info("Form submission received for form id={}, language={}, IP={}",
                formId, language, request.getRemoteAddr());

        try {
            // Load form definition for validation
            FormDefinition formDefinition = formDefinitionService.getFormById(formId);

            // Extract form data from request
            Map<String, Object> formData = extractFormData(request);

            log.debug("Extracted form data: {}", formData);

            // Validate form data against JSON Schema
            FormValidationService.ValidationResult validationResult =
                    validationService.validateFormData(formData, formDefinition.getSchema());

            if (!validationResult.isValid()) {
                log.warn("Form validation failed for form {}: {}", formId, validationResult.getAllErrors());

                // Preserve form data and show errors - return to form view directly (no redirect)
                return showFormWithValidationErrors(formId, language, formData, validationResult, model, request);
            }

            log.debug("Form data validation passed");

            // Process submission: create Patient and FormSubmission entities
            FormSubmission submission = submissionService.processSubmission(
                    formId,
                    formData,
                    language,
                    request
            );

            log.info("Form submission saved successfully: submissionId={}, patientId={}",
                    submission.getId(), submission.getPatient().getId());

            // TODO: Generate PDF asynchronously (low priority - current synchronous approach works fine)

            // Redirect to success page with submission ID
            return "redirect:/forms/" + formId + "/success?lang=" + language +
                    "&submissionId=" + submission.getId();

        } catch (IllegalArgumentException e) {
            // Validation error (missing required patient fields)
            log.warn("Form validation failed for form {}: {}", formId, e.getMessage());

            // Extract form data to preserve it
            Map<String, Object> formData = extractFormData(request);

            // Create a validation result with global error
            FormValidationService.ValidationResult validationResult =
                    FormValidationService.ValidationResult.failure(
                            Collections.emptyMap(),
                            Collections.singletonList(e.getMessage())
                    );

            try {
                return showFormWithValidationErrors(formId, language, formData, validationResult, model, request);
            } catch (Exception ex) {
                return "redirect:/forms/" + formId + "?lang=" + language + "&error=validation_error";
            }

        } catch (Exception e) {
            log.error("Form submission failed for form {}", formId, e);
            return "redirect:/forms/" + formId + "?lang=" + language + "&error=submission_error";
        }
    }

    /**
     * Displays success page after form submission.
     * GET /forms/{id}/success
     */
    @GetMapping("/forms/{id}/success")
    public String showSuccess(@PathVariable("id") UUID formId,
                              @RequestParam(value = "lang", defaultValue = "de") String language,
                              @RequestParam(value = "submissionId", required = false) UUID submissionId,
                              Model model) {

        log.info("Displaying success page for form {}, submissionId={}", formId, submissionId);

        try {
            FormDefinition formDefinition = formDefinitionService.getFormById(formId);

            model.addAttribute("formDefinition", formDefinition);
            model.addAttribute("language", language);
            model.addAttribute("submissionId", submissionId);

            // If submissionId is provided, load submission details
            if (submissionId != null) {
                try {
                    FormSubmission submission = submissionService.getSubmissionById(submissionId);
                    model.addAttribute("submission", submission);
                    model.addAttribute("patient", submission.getPatient());
                } catch (Exception e) {
                    log.warn("Could not load submission details: {}", e.getMessage());
                }
            }

            return "forms/submission-success";

        } catch (Exception e) {
            log.error("Failed to display success page for form {}", formId, e);
            return "redirect:/";
        }
    }

    /**
     * Generates and serves PDF for a form submission.
     * GET /forms/{id}/pdf?submissionId=XXX
     *
     * @param formId       UUID of the form definition
     * @param submissionId UUID of the form submission
     * @param response     HTTP response (for serving PDF)
     */
    @GetMapping("/forms/{id}/pdf")
    public void generatePdf(@PathVariable("id") UUID formId,
                            @RequestParam("submissionId") UUID submissionId,
                            HttpServletResponse response) {

        log.info("PDF generation requested for submission {}", submissionId);

        try {
            // Generate and serve PDF from submission
            pdfService.generateAndServePdfFromSubmission(submissionId, response);

            log.info("PDF successfully generated and served for submission {}", submissionId);

        } catch (Exception e) {
            log.error("Failed to generate PDF for submission {}", submissionId, e);

            // Set error response
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Error generating PDF: " + e.getMessage());
            } catch (Exception writeException) {
                log.error("Failed to write error response", writeException);
            }
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Shows form with validation errors and preserved data.
     */
    private String showFormWithValidationErrors(UUID formId, String language,
                                                 Map<String, Object> formData,
                                                 FormValidationService.ValidationResult validationResult,
                                                 Model model, HttpServletRequest request) {
        try {
            // Load form definition
            FormDefinition formDefinition = formDefinitionService.getFormById(formId);

            // Load translation if available
            FormTranslationDTO translation = null;
            try {
                translation = translationService.getTranslation(formId, language);
            } catch (Exception e) {
                log.warn("Translation not found for form {} in language {}", formId, language);
            }

            // Generate dynamic field HTML
            String dynamicFieldsHtml = templateGenerator.generateFieldsOnly(
                    formDefinition,
                    language,
                    translation
            );

            // Add model attributes
            model.addAttribute("formDefinition", formDefinition);
            model.addAttribute("formData", formData);  // Preserved data (Map)
            model.addAttribute("formDataMap", formData);  // Pass Map directly for Thymeleaf JavaScript inlining
            model.addAttribute("validationErrorsMap", validationResult.getFieldErrors());  // Pass Map directly
            model.addAttribute("dynamicFieldsHtml", dynamicFieldsHtml);
            model.addAttribute("language", language);
            model.addAttribute("isRtl", "ar".equalsIgnoreCase(language));
            model.addAttribute("error", true);
            model.addAttribute("validationErrors", validationResult.getFieldErrors());
            model.addAttribute("globalErrors", validationResult.getGlobalErrors());

            // Debug logging
            log.debug("Preserved formData for validation failure: {}", formData);
            log.debug("Field errors: {}", validationResult.getFieldErrors());

            // Build user-friendly error message
            model.addAttribute("errorMessage", buildValidationErrorMessage(validationResult, language));

            return "base/dynamic-form-layout";

        } catch (Exception e) {
            log.error("Failed to display form with validation errors", e);
            return "redirect:/forms/" + formId + "?lang=" + language + "&error=validation_error";
        }
    }

    /**
     * Builds user-friendly validation error message from validation result.
     */
    private String buildValidationErrorMessage(FormValidationService.ValidationResult validationResult,
                                                String language) {
        StringBuilder message = new StringBuilder();

        // Get localized header
        String header = switch (language.toLowerCase()) {
            case "de" -> "Bitte korrigieren Sie folgende Fehler:";
            case "en" -> "Please correct the following errors:";
            case "ar" -> "يرجى تصحيح الأخطاء التالية:";
            case "ru" -> "Пожалуйста, исправьте следующие ошибки:";
            default -> "Please correct the following errors:";
        };

        message.append(header).append("\n");

        // Add field-specific errors
        validationResult.getFieldErrors().forEach((field, errors) -> {
            for (String error : errors) {
                message.append("• ").append(translateFieldName(field, language))
                        .append(": ").append(translateErrorMessage(error, language)).append("\n");
            }
        });

        // Add global errors
        for (String error : validationResult.getGlobalErrors()) {
            message.append("• ").append(translateErrorMessage(error, language)).append("\n");
        }

        return message.toString();
    }

    /**
     * Translates field name to user-friendly format.
     */
    private String translateFieldName(String fieldName, String language) {
        // Convert camelCase to readable format
        String readable = fieldName.replaceAll("([A-Z])", " $1")
                .replaceAll("_", " ")
                .trim();
        return readable.substring(0, 1).toUpperCase() + readable.substring(1);
    }

    /**
     * Translates JSON Schema error message to user-friendly message.
     */
    private String translateErrorMessage(String errorMessage, String language) {
        // Extract the actual error from JSON Schema format (e.g., "$.firstName: must be...")
        String cleanMessage = errorMessage;
        if (errorMessage.contains(": ")) {
            cleanMessage = errorMessage.substring(errorMessage.lastIndexOf(": ") + 2);
        }

        // Return the cleaned message (JSON Schema validator already provides localized messages)
        return cleanMessage;
    }

    /**
     * Extracts form data from HTTP request parameters.
     */
    private Map<String, Object> extractFormData(HttpServletRequest request) {
        Map<String, Object> formData = new HashMap<>();

        request.getParameterMap().forEach((key, values) -> {
            if (values.length == 1) {
                formData.put(key, values[0]);
            } else {
                formData.put(key, values);
            }
        });

        // Remove CSRF token and other Spring Security params
        formData.remove("_csrf");

        return formData;
    }

    /**
     * Gets localized error message.
     */
    private String getErrorMessage(String errorCode, String language) {
        if (errorCode == null) {
            return null;
        }

        return switch (language.toLowerCase()) {
            case "de" -> switch (errorCode) {
                case "submission_error" -> "Fehler beim Absenden des Formulars. Bitte versuchen Sie es erneut.";
                case "validation_error" -> "Bitte überprüfen Sie Ihre Eingaben.";
                case "not_found" -> "Formular nicht gefunden.";
                default -> "Ein Fehler ist aufgetreten.";
            };
            case "en" -> switch (errorCode) {
                case "submission_error" -> "Error submitting form. Please try again.";
                case "validation_error" -> "Please check your input.";
                case "not_found" -> "Form not found.";
                default -> "An error occurred.";
            };
            case "ar" -> switch (errorCode) {
                case "submission_error" -> "خطأ في إرسال النموذج. يرجى المحاولة مرة أخرى.";
                case "validation_error" -> "يرجى التحقق من المدخلات.";
                case "not_found" -> "النموذج غير موجود.";
                default -> "حدث خطأ.";
            };
            case "ru" -> switch (errorCode) {
                case "submission_error" -> "Ошибка при отправке формы. Пожалуйста, попробуйте еще раз.";
                case "validation_error" -> "Пожалуйста, проверьте ваш ввод.";
                case "not_found" -> "Форма не найдена.";
                default -> "Произошла ошибка.";
            };
            default -> "An error occurred.";
        };
    }
}
