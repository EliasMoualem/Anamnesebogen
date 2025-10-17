package de.elias.moualem.Anamnesebogen.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * Custom error controller to handle application errors gracefully.
 * This replaces the default Whitelabel Error Page with a redirect to the main form.
 */
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exceptionObj = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Log the error details
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            log.error("Error with status code: {}", statusCode);
        }

        if (exceptionObj instanceof Exception exception) {
            log.error("Error exception: ", exception);
        }

        try {
            // Redirect back to the main form with a generic error message
            response.sendRedirect("/anamnesebogen?error=system_error");
        } catch (IOException e) {
            log.error("Failed to redirect from error page", e);
        }
    }
}