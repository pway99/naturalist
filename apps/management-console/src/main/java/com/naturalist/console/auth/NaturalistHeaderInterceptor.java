package com.naturalist.console.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Publishes the current session identity and CSRF token as plain request attributes for
 * the shared {@code layout/page.jte} header, which is compiled against every domain-console
 * classpath and so may not reference Spring-Security or app-package types. Identity is read
 * through the {@link CurrentNaturalistView} seam.
 */
@Component
public class NaturalistHeaderInterceptor implements HandlerInterceptor {

    static final String DISPLAY_NAME = "naturalistDisplayName";
    static final String AUTHENTICATED = "naturalistAuthenticated";
    static final String CSRF_PARAM = "naturalistCsrfParam";
    static final String CSRF_TOKEN = "naturalistCsrfToken";

    /** Header label for an authenticated session with no naturalist identity (the config admin). */
    static final String ADMIN_LABEL = "Administrator";

    /** Request-attribute key read by insects-console (same literal, by convention). */
    static final String CURRENT_NATURALIST_NAME = "naturalist.currentNaturalistName";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        boolean authenticated = CurrentNaturalistView.isAuthenticated();
        String displayName = CurrentNaturalistView.displayName();
        if (displayName == null && authenticated) {
            // Authenticated but no NaturalistPrincipal == the config admin; give it a label.
            displayName = ADMIN_LABEL;
        }
        request.setAttribute(DISPLAY_NAME, displayName);
        request.setAttribute(AUTHENTICATED, authenticated);
        request.setAttribute(CURRENT_NATURALIST_NAME, CurrentNaturalistView.currentNaturalistSlug());
        CsrfToken csrf = CurrentNaturalistView.csrfToken();
        if (csrf != null) {
            request.setAttribute(CSRF_PARAM, csrf.getParameterName());
            request.setAttribute(CSRF_TOKEN, csrf.getToken());
        }
        return true;
    }
}
