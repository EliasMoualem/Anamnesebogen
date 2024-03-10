package de.elias.moualem.Anamnesebogen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafConfig {

    @Bean
    public ClassLoaderTemplateResolver bogenTemplateResolver(){
        ClassLoaderTemplateResolver pdfTemplateResolver = new ClassLoaderTemplateResolver();
        pdfTemplateResolver.setPrefix("templates/"); // Location of thymeleaf template
        pdfTemplateResolver.setSuffix(".html"); // Template file extension
        pdfTemplateResolver.setTemplateMode("HTML"); // Template Type
        pdfTemplateResolver.setCharacterEncoding("UTF-8");
        pdfTemplateResolver.setCacheable(false); // Turning of cache to facilitate template changes
        pdfTemplateResolver.setOrder(1);

        return pdfTemplateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(bogenTemplateResolver());

        return engine;
    }
}
