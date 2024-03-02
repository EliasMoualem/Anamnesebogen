package de.elias.moualem.Anamnesebogen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafConfig {

    @Bean
    public ClassLoaderTemplateResolver bogenTemplateResolver(){
        ClassLoaderTemplateResolver pdfTemplateResolver = new ClassLoaderTemplateResolver();
        pdfTemplateResolver.setPrefix("templates/");
        pdfTemplateResolver.setSuffix(".html");
        pdfTemplateResolver.setTemplateMode("HTML");
        pdfTemplateResolver.setCharacterEncoding("UTF-8");
        pdfTemplateResolver.setOrder(1);
        return pdfTemplateResolver;
    }
}
