package de.elias.moualem.Anamnesebogen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 * Allows the React frontend (running on different port) to call the backend API.
 *
 * Development Setup:
 * - React dev server: http://localhost:3000 (Create React App default)
 * - React dev server: http://localhost:5173 (Vite default)
 * - Backend API: http://localhost:8080
 *
 * Production:
 * Update allowedOrigins with your production domain.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // Allow React dev servers (both CRA and Vite defaults)
                .allowedOrigins(
                        "http://localhost:3000",  // Create React App default
                        "http://localhost:5173",  // Vite default
                        "http://localhost:4173",  // Vite preview default
                        "http://localhost:5175"   // Current Vite dev server
                )
                // Allow common HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // Allow common headers
                .allowedHeaders("*")
                // Allow credentials (cookies, authorization headers)
                // Required for future authentication implementation
                .allowCredentials(true)
                // Cache preflight requests for 1 hour
                .maxAge(3600);
    }
}
