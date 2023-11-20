package de.elias.moualem.Aufnahmebogen.controller;

import com.lowagie.text.DocumentException;
import de.elias.moualem.Aufnahmebogen.model.Patient;
import de.elias.moualem.Aufnahmebogen.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class AnamnesebogenController {

    private final PdfService pdfService;

    @Autowired
    public AnamnesebogenController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/anamnesebogen")
    public String anamnesebogen(Model model) {
        model.addAttribute("patient", new Patient());
        return "anamnesebogen";
    }

    @GetMapping("/pdf")
    public void createAndStorePDF(HttpServletResponse response, @Valid Patient patient) {
        try {
            Path file = Paths.get(pdfService.generatePdf(patient).getAbsolutePath());
            if (Files.exists(file)) {
                response.setContentType("application/pdf");
                response.addHeader("Content-Disposition",
                        "attachment; filename=" + file.getFileName());
                Files.copy(file, response.getOutputStream());
                response.getOutputStream().flush();
            }
        } catch (DocumentException | IOException ex) {
            //TODO: @slf4j
        }
    }
}
