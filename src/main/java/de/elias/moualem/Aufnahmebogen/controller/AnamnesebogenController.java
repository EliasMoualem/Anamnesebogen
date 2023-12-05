package de.elias.moualem.Aufnahmebogen.controller;

import com.lowagie.text.DocumentException;
import de.elias.moualem.Aufnahmebogen.model.MinorPatient;
import de.elias.moualem.Aufnahmebogen.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AnamnesebogenController {

    private final PdfService pdfService;

    @GetMapping("/anamnesebogen")
    public String anamnesebogen(Model model) {
        model.addAttribute("minorPatient", new MinorPatient());
        return "anamnesebogen";
    }

    @GetMapping("/pdf")
    public void createAndStorePDF(HttpServletResponse response, @Valid MinorPatient minorPatient) {
        try {
            Path file = Paths.get(pdfService.generatePdf(minorPatient).getAbsolutePath());
            if (Files.exists(file)) {
                response.setContentType("application/pdf");
                response.addHeader("Content-Disposition",
                        "attachment; filename=" + file.getFileName());
                Files.copy(file, response.getOutputStream());
                response.getOutputStream().flush();
            }
        } catch (DocumentException | IOException ex) {
            log.error("DocumentException|IOException was thrown", ex);
        }
    }
}
