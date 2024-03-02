package de.elias.moualem.Anamnesebogen.service;

import com.lowagie.text.DocumentException;
import de.elias.moualem.Anamnesebogen.model.MinorPatient;
import io.micrometer.core.annotation.Timed;
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
@Timed
public class PdfService {

    //private static final String PDF_RESOURCES = "/pdf-resources/";
    private final SpringTemplateEngine templateEngine;

    public File generatePdf(MinorPatient minorPatient) throws IOException, DocumentException {
        Context context = getContext(minorPatient);
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

    private Context getContext(MinorPatient minorPatient) {
        Context context = new Context();
        context.setVariable("minorPatient", minorPatient);
        return context;
    }

    private String loadAndFillTemplate(Context context) {
        return templateEngine.process("pdf_anamnesebogen", context);
    }
}
