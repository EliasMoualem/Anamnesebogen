package de.elias.moualem.Anamnesebogen.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Locale Configuration for i18n support
 * Configures message source and locale resolution from Accept-Language header
 */
@Configuration
public class LocaleConfig {

    /**
     * Configure MessageSource for loading messages from properties files
     * Supports messages_de.properties and messages_en.properties
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.GERMAN); // German as default
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true); // Return code if message not found
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        return messageSource;
    }

    /**
     * Configure LocaleResolver to extract locale from Accept-Language header
     * Supports: de (German), en (English)
     * Falls back to German if language not supported
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.GERMAN); // Default to German
        localeResolver.setSupportedLocales(List.of(
                Locale.GERMAN,
                Locale.ENGLISH
        ));
        return localeResolver;
    }
}
