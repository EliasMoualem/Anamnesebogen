package de.elias.moualem.Aufnahmebogen.service;

import com.lowagie.text.DocumentException;
import de.elias.moualem.Aufnahmebogen.model.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Service
@RequiredArgsConstructor
public class PdfService {

    //private static final String PDF_RESOURCES = "/pdf-resources/";
    private final SpringTemplateEngine templateEngine;

    public File generatePdf(Patient patient) throws IOException, DocumentException {
        Context context = getContext(patient);
        String html = loadAndFillTemplate(context);
        return renderPdf(html);
    }

    private File renderPdf(String html) throws IOException, DocumentException {
        //String fileName = patient.getLastName() + ", " + patient.getFirstName() + " Anamnesebogen";
        File file = File.createTempFile("Anamnesebogen", ".pdf");
        OutputStream outputStream = new FileOutputStream(file);
        ITextRenderer renderer = new ITextRenderer(20f * 4f / 3f, 20);
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);
        outputStream.close();
        file.deleteOnExit();
        return file;
    }

    private Context getContext(Patient patient) {
        Context context = new Context();
        context.setVariable("patient", patient);
        return context;
    }

    private String loadAndFillTemplate(Context context) {
        return templateEngine.process("pdf_anamnesebogen", context);
    }
}
