package com.naturalist.console.admin;

import com.naturalist.account.Account;
import com.naturalist.account.AccountCommand;
import com.naturalist.account.AccountName;
import com.naturalist.account.AccountNotFoundException;
import com.naturalist.account.AccountQuery;
import com.naturalist.console.auth.AccountSessions;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.function.Function;

/**
 * Renders {@code /admin/accounts}: a paged list of accounts with their email, verification,
 * access and status, and the four account changes an operator makes without SQL on the
 * production database: grant and revoke {@code VISION}, suspend and reinstate (launch item 1.9;
 * the request-mode grant UI of the accounts design). {@code /admin/**} is {@code ADMIN} only in
 * {@code SecurityConfiguration}; every change is a CSRF-protected POST.
 *
 * <p>Suspend is a ban: after the command, every live session of the account is ended through
 * {@link AccountSessions}, so the suspended naturalist is signed out on their next request and
 * cannot sign back in. Revoke leaves sessions alone; the session keeps its {@code VISION}
 * authority until the naturalist next signs in.
 *
 * <p>Each change checks its input at the edge before calling the command (B1): an account name
 * that is not a valid slug, or one that names no account, is observed at {@code WARN} and answered
 * with a redirect that carries the message. An account that disappears between that check and
 * the command is refused by the command, which already counted it ({@code accountKnown}), so the
 * edge answers without counting it again.
 */
@Controller
public class AdminAccountsController {

    private static final Observer observer = Observer.forClass(AdminAccountsController.class);

    private final AccountQuery accountQuery;
    private final AccountCommand accountCommand;
    private final AccountSessions accountSessions;

    AdminAccountsController(AccountQuery accountQuery, AccountCommand accountCommand,
                            AccountSessions accountSessions) {
        this.accountQuery = accountQuery;
        this.accountCommand = accountCommand;
        this.accountSessions = accountSessions;
    }

    @GetMapping("/admin/accounts")
    String accounts(@RequestParam(value = "page", defaultValue = "0") int page,
                    HttpServletRequest request, Model model) {
        Page<Account> accounts = accountQuery.findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("accounts", accounts);
        if (!model.containsAttribute("notice")) {
            model.addAttribute("notice", "");
        }
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
        return "admin/accounts";
    }

    @PostMapping("/admin/accounts/{account}/grant-vision")
    String grantVision(@PathVariable("account") AccountName account,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       RedirectAttributes redirect) {
        return change(observer.forMethod("grantVision"), account, page, redirect,
                found -> {
                    accountCommand.grantVision(found.name());
                    return "Granted vision identification to " + found.email() + ".";
                });
    }

    @PostMapping("/admin/accounts/{account}/revoke-vision")
    String revokeVision(@PathVariable("account") AccountName account,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        RedirectAttributes redirect) {
        return change(observer.forMethod("revokeVision"), account, page, redirect,
                found -> {
                    accountCommand.revokeVision(found.name());
                    return "Revoked vision identification from " + found.email() + ".";
                });
    }

    @PostMapping("/admin/accounts/{account}/suspend")
    String suspend(@PathVariable("account") AccountName account,
                   @RequestParam(value = "page", defaultValue = "0") int page,
                   RedirectAttributes redirect) {
        return change(observer.forMethod("suspend"), account, page, redirect,
                found -> {
                    accountCommand.suspend(found.name());
                    int ended = accountSessions.endAll(found.email());
                    return "Suspended " + found.email() + "; ended " + ended
                            + (ended == 1 ? " live session." : " live sessions.");
                });
    }

    @PostMapping("/admin/accounts/{account}/reinstate")
    String reinstate(@PathVariable("account") AccountName account,
                     @RequestParam(value = "page", defaultValue = "0") int page,
                     RedirectAttributes redirect) {
        return change(observer.forMethod("reinstate"), account, page, redirect,
                found -> {
                    accountCommand.reinstate(found.name());
                    return "Reinstated " + found.email() + ".";
                });
    }

    /**
     * The shared shape of the four changes: refuse a malformed or unknown account at the edge,
     * apply the change (which returns the notice to show), and redirect back to the page the operator was on with a notice.
     */
    private String change(MethodObserver methodObserver, AccountName account, int page,
                          RedirectAttributes redirect, Function<Account, String> apply) {
        String back = "redirect:/admin/accounts?page=" + Math.max(0, page);
        InvariantObservation wellFormed = methodObserver
                .arguments(i -> i.entityName(account, "account"))
                .observe(Level.WARN);
        if (!wellFormed.violations().isEmpty()) {
            redirect.addFlashAttribute("notice", "That is not a valid account name.");
            return back;
        }
        Optional<Account> found = accountQuery.getByName(account);
        InvariantObservation known = methodObserver
                .state(i -> i.isTrue(found.isPresent(), "accountKnown"))
                .observe(Level.WARN);
        if (!known.violations().isEmpty()) {
            redirect.addFlashAttribute("notice", "No account is named " + account.value() + ".");
            return back;
        }
        String notice;
        try {
            notice = apply.apply(found.get());
        } catch (AccountNotFoundException goneSinceListed) {
            // Already counted by the command's accountKnown decision; answer without a second count.
            redirect.addFlashAttribute("notice", "No account is named " + account.value() + ".");
            return back;
        }
        redirect.addFlashAttribute("notice", notice);
        return back;
    }
}
