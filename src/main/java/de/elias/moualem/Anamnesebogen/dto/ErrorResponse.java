package de.elias.moualem.Anamnesebogen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized error response DTO for REST API.
 * Provides consistent error format for frontend clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * HTTP status reason phrase (e.g., "Not Found", "Bad Request").
     */
    private String error;

    /**
     * Detailed error message for developers.
     */
    private String message;

    /**
     * Request path that caused the error.
     */
    private String path;

    /**
     * Optional: Stack trace for debugging (only in development mode).
     */
    private String trace;
}
