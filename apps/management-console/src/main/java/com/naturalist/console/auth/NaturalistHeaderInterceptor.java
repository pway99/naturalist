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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(DISPLAY_NAME, CurrentNaturalistView.displayName());
        request.setAttribute(AUTHENTICATED, CurrentNaturalistView.isAuthenticated());
        Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
        if (csrfAttr instanceof CsrfToken csrf) {
            request.setAttribute(CSRF_PARAM, csrf.getParameterName());
            request.setAttribute(CSRF_TOKEN, csrf.getToken());
        }
        return true;
    }
}
