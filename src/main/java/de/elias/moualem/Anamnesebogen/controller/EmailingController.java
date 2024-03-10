package de.elias.moualem.Anamnesebogen.controller;

import de.elias.moualem.Anamnesebogen.dto.MailRequest;
import de.elias.moualem.Anamnesebogen.service.EmailingService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mail")
public class EmailingController {

    private final EmailingService emailingService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void sendMail(@RequestBody MailRequest request) throws MessagingException {
        emailingService.sendMail(request);
    }
}
