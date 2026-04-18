package com.naturalist.console;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;

@Controller
class HomeController {

    @GetMapping("/")
    String home() {
        return "home";
    }

    @GetMapping("/login")
    String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("error", error != null ? error : "");
        model.addAttribute("logout", logout != null ? logout : "");
        var csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
        return "login";
    }
}
