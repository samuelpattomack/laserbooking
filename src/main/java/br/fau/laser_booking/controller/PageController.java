package br.fau.laser_booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/app";
    }

    @GetMapping("/login")
    public String login() {
        return "login";   // templates/login.html
    }

    @GetMapping("/app")
    public String app() {
        return "app";     // templates/app.html (sua página UC01/UC02/UC03)
    }
}
