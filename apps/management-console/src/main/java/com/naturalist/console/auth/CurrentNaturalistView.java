package com.naturalist.console.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Template-facing accessor for session identity, called from {@code layout/page.jte}.
 * A sanctioned SecurityContext reader (design: seam discipline). Static because JTE
 * templates cannot inject beans; the read is stateless.
 */
public final class CurrentNaturalistView {

    private CurrentNaturalistView() {
    }

    /** The current naturalist's given name, or {@code null} for admin/anonymous. */
    public static String displayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NaturalistPrincipal p) {
            return p.givenName();
        }
        return null;
    }

    /** Whether the request is authenticated (naturalist or admin), not anonymous. */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** The CSRF token for the current request, or {@code null} outside a request. */
    public static CsrfToken csrfToken() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            return (CsrfToken) sra.getRequest().getAttribute(CsrfToken.class.getName());
        }
        return null;
    }

    /** The current naturalist's slug (NaturalistName value), or {@code null} for admin/anonymous. */
    public static String currentNaturalistSlug() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NaturalistPrincipal p) {
            return p.naturalistName().value();
        }
        return null;
    }
}
