package de.elias.moualem.Aufnahmebogen.controller;

import de.elias.moualem.Aufnahmebogen.model.Patient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class GreetingController {

    @GetMapping("/anamnesebogen")
    public String greetingForm(Model model) {
        model.addAttribute("patient", new Patient());
        return "anamnesebogen";
    }

    @PostMapping("/x")
    public String greetingSubmit(@ModelAttribute Patient patient, Model model) {
        model.addAttribute("patient", patient);

        //create pdf and store result

        return "result";
    }

}
