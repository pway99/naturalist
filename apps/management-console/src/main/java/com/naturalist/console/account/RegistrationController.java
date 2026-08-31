package com.naturalist.console.account;

import com.naturalist.account.AccountCommand;
import com.naturalist.account.DuplicateEmailException;
import com.naturalist.account.InvalidTokenException;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Public self-registration and email verification. Both routes are {@code permitAll} in
 * {@link com.naturalist.console.SecurityConfiguration}.
 *
 * <p><strong>Register</strong> runs the two-domain {@link RegistrationTransaction}, then (as
 * post-commit side effects) emails the verification link through the {@link EmailSender} seam
 * and <em>auto-logs-in</em> the new naturalist so they browse immediately as
 * {@code ROLE_NATURALIST} — without {@code VISION}, which arrives on verification.
 *
 * <p><strong>Verify</strong> redeems the token and, if the verifier is the signed-in owner,
 * re-derives their session authorities in place so a self-serve {@code VISION} grant takes
 * effect without a re-login.
 *
 * <p>The programmatic login builds an authenticated token from the freshly-loaded principal
 * (no password re-check — the user just set it) and persists it via the
 * {@link SecurityContextRepository}, rotating the session id first to close the
 * session-fixation window that bypassing the login filter would otherwise leave open.
 */
@Controller
class RegistrationController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final RegistrationTransaction registration;
    private final EmailSender emailSender;
    private final UserDetailsService userDetailsService;
    private final AccountCommand accountCommand;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    RegistrationController(RegistrationTransaction registration,
                           EmailSender emailSender,
                           UserDetailsService userDetailsService,
                           AccountCommand accountCommand) {
        this.registration = registration;
        this.emailSender = emailSender;
        this.userDetailsService = userDetailsService;
        this.accountCommand = accountCommand;
    }

    @GetMapping("/register")
    String form(@RequestParam(value = "registered", required = false) String registered,
                HttpServletRequest request, Model model) {
        model.addAttribute("registered", registered != null);
        model.addAttribute("error", "");
        model.addAttribute("email", "");
        model.addAttribute("publicHandle", "");
        model.addAttribute("givenName", "");
        model.addAttribute("familyName", "");
        putCsrf(request, model);
        return "register";
    }

    @PostMapping("/register")
    String submit(@RequestParam String email,
                  @RequestParam String password,
                  @RequestParam String publicHandle,
                  @RequestParam String givenName,
                  @RequestParam(required = false) String familyName,
                  HttpServletRequest request, HttpServletResponse response, Model model) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            // The account port only ever sees the encoded hash (never blank), so it cannot
            // enforce a minimum length — this app-layer policy does, server-side, so the
            // client-side minlength on the form cannot be the only guard.
            return reRender(model, request,
                    "Please choose a password of at least " + MIN_PASSWORD_LENGTH + " characters.",
                    email, publicHandle, givenName, familyName);
        }
        RegistrationTransaction.Result result;
        try {
            result = registration.register(email, password, publicHandle, givenName, blankToNull(familyName));
        } catch (DuplicateEmailException alreadyRegistered) {
            return reRender(model, request,
                    "That email is already registered — try logging in instead.",
                    email, publicHandle, givenName, familyName);
        } catch (InvariantViolationException invalid) {
            return reRender(model, request,
                    "Please check your details: a valid email, a password of at least 8 characters, "
                            + "and an unused handle are required.",
                    email, publicHandle, givenName, familyName);
        }

        String verifyUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/verify").queryParam("token", result.rawToken()).toUriString();
        emailSender.send(new EmailMessage(
                result.email(),
                "Verify your email — The Amateur Naturalist",
                "Welcome! Confirm your email address to unlock vision identification:\n\n"
                        + verifyUrl + "\n\nIf you didn't sign up, you can ignore this message."));

        autoLogin(result.email(), request, response);
        return "redirect:/register?registered";
    }

    @GetMapping("/verify")
    String verify(@RequestParam(value = "token", required = false) String token,
                  HttpServletRequest request, HttpServletResponse response, Model model) {
        boolean success;
        String message;
        if (token == null || token.isBlank()) {
            success = false;
            message = "This verification link is missing its token.";
        } else {
            try {
                accountCommand.verifyEmail(token);
                success = true;
                message = "Your email is verified. Vision identification is now available.";
                reDeriveAuthorities(request, response);
            } catch (InvalidTokenException invalidOrExpired) {
                success = false;
                message = "This verification link is invalid or has expired. "
                        + "Request a fresh one from your profile.";
            }
        }
        model.addAttribute("success", success);
        model.addAttribute("message", message);
        return "verify";
    }

    /** Auto-login after registration: a full login transition, so rotate the session id. */
    private void autoLogin(String email, HttpServletRequest request, HttpServletResponse response) {
        UserDetails principal = userDetailsService.loadUserByUsername(email);
        establishSession(principal, request, response, true);
    }

    /**
     * Refresh the signed-in owner's authorities in place after verification (picks up a
     * self-serve VISION grant). No-op when the verifier is anonymous or is the config admin;
     * not a login transition, so the session id is left alone.
     */
    private void reDeriveAuthorities(HttpServletRequest request, HttpServletResponse response) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null || !current.isAuthenticated()
                || current instanceof AnonymousAuthenticationToken) {
            return;
        }
        try {
            UserDetails refreshed = userDetailsService.loadUserByUsername(current.getName());
            establishSession(refreshed, request, response, false);
        } catch (UsernameNotFoundException unresolved) {
            // Current principal no longer resolvable here (e.g. the config admin) — leave the session as-is.
        }
    }

    private void establishSession(UserDetails principal, HttpServletRequest request,
                                  HttpServletResponse response, boolean rotateSession) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (rotateSession) {
            request.getSession();          // ensure one exists (CSRF usually made it already)
            request.changeSessionId();     // new id → closes the session-fixation window
        }
        securityContextRepository.saveContext(context, request, response);
    }

    private String reRender(Model model, HttpServletRequest request, String error,
                            String email, String publicHandle, String givenName, String familyName) {
        model.addAttribute("registered", false);
        model.addAttribute("error", error);
        model.addAttribute("email", nullToBlank(email));
        model.addAttribute("publicHandle", nullToBlank(publicHandle));
        model.addAttribute("givenName", nullToBlank(givenName));
        model.addAttribute("familyName", nullToBlank(familyName));
        putCsrf(request, model);
        return "register";
    }

    private static void putCsrf(HttpServletRequest request, Model model) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
