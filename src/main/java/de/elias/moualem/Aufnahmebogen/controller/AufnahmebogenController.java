package de.elias.moualem.Aufnahmebogen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AufnahmebogenController {
    @GetMapping("/aufnahmebogen")
    public String homeFreemarker(Model model) {
        model.addAttribute("message", "Welcome to the Demo using Freemarker!");
        return "aufnahmebogen";
    }
}