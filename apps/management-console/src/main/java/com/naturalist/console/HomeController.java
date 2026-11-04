package com.naturalist.console;

import com.naturalist.console.challenge.ChallengeGate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class HomeController {

    private final ChallengeGate challenge;

    HomeController(ChallengeGate challenge) {
        this.challenge = challenge;
    }

    @GetMapping("/")
    String home() {
        return "home";
    }

    @GetMapping("/login")
    String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "challenge", required = false) String challengeFailed,
            @RequestParam(value = "unavailable", required = false) String challengeUnavailable,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("error", error != null ? error : "");
        model.addAttribute("logout", logout != null ? logout : "");
        model.addAttribute("challengeSiteKey", challenge.siteKey());
        // Fixed text chosen by which parameter is present: the query value is never rendered.
        if (challengeUnavailable != null) {
            model.addAttribute("challengeMessage",
                    "We couldn't confirm your browser just now — please try again in a moment.");
        } else if (challengeFailed != null) {
            model.addAttribute("challengeMessage", "Please complete the verification and try again.");
        }
        var csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
        return "login";
    }
}
