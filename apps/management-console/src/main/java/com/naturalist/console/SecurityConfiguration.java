package com.naturalist.console;

import com.naturalist.console.admin.AdminProperties;
import com.naturalist.console.auth.CurrentNaturalist;
import com.naturalist.console.auth.IdentifyRateLimitFilter;
import com.naturalist.console.auth.LoginChallengeFilter;
import com.naturalist.console.auth.LoginRateLimitFilter;
import com.naturalist.console.challenge.ChallengeGate;
import com.naturalist.resilience.Resilience;
import com.naturalist.usage.IdentificationBudget;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.context.support.WebApplicationContextUtils;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AdminProperties.class)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, Resilience resilience,
                                    CurrentNaturalist currentNaturalist,
                                    IdentificationBudget identificationBudget,
                                    ChallengeGate challengeGate) throws Exception {
        return http
                // Global ceiling on POST /login (auth.login): caps the BCrypt CPU a
                // credential-stuffing run can burn; rejections are observed as withinRate.
                .addFilterBefore(new LoginRateLimitFilter(resilience), UsernamePasswordAuthenticationFilter.class)
                // The bot challenge, after the limits and before the password check (ADR-027 D7).
                .addFilterAfter(new LoginChallengeFilter(challengeGate), LoginRateLimitFilter.class)
                // Per-client ceiling on POST /<domain>/identify (vision.identification.client, two a
                // minute per address). After AuthorizationFilter, so only a request that already
                // passed authentication, CSRF and the VISION check spends a client permit; a
                // refusal is recorded against the signed-in naturalist (identification-refused).
                .addFilterAfter(new IdentifyRateLimitFilter(resilience, currentNaturalist, identificationBudget),
                        AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Static assets + entry / self-service auth pages — open to everyone. So is
                        // /error: behind a login, a page that throws redirects to /login instead of
                        // reporting the failure, and a broken login page redirects to itself.
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/error",
                                "/login", "/register", "/verify", "/reset/**").permitAll()
                        // The paid vision feature: identification requires the VISION entitlement.
                        // (Anonymous callers are bounced to login; a signed-in BROWSE_ONLY
                        // naturalist gets 403 — they lack the authority.)
                        .requestMatchers(HttpMethod.POST,
                                "/insects/identify", "/plants/identify")
                                .hasAuthority("VISION")
                        // Anonymous browsing of the public catalog: the organism domains and the
                        // field-guide library. Operational Oak Vista domains stay authenticated
                        // (they fall through to anyRequest below).
                        .requestMatchers(HttpMethod.GET,
                                "/insects/**", "/plants/**",
                                "/citations/**", "/concepts/**", "/glossary/**").permitAll()
                        // Admin console (GET and POST).
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // A naturalist's own profile / handle management.
                        .requestMatchers("/naturalists/me/**").authenticated()
                        // Everything else — member write actions (observe, add image, notes) and
                        // the operational domains — requires a signed-in naturalist.
                        .anyRequest().authenticated()
                )
                // The single-use /verify token travels in a GET query string; never let a
                // browser forward it to a third party in a Referer header.
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        // Page scripts cannot reach a vendor host and forms cannot post elsewhere.
                        // Deliberately not default-src: the templates use inline styles and scripts,
                        // and ADR-027 loads Turnstile's script and frame; a stricter policy is a
                        // template effort of its own (design plan section 8, item 1).
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "connect-src 'self'; form-action 'self'; frame-ancestors 'none'"))
                        .contentTypeOptions(Customizer.withDefaults()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static boolean onManagementServer(HttpServletRequest request) {
        return WebServerApplicationContext.hasServerNamespace(
                WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()), "management");
    }

    /**
     * The management server is open, because nothing can reach its port except over the private
     * network, and it exposes only health and read-only metrics. Security's filter also runs there, so
     * without this chain Prometheus would be redirected to the login page. An actuator path anywhere
     * else is refused, including for a signed-in user, so setting the two ports equal by mistake
     * cannot publish the metrics.
     */
    @Bean
    @Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain managementFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(request -> onManagementServer(request)
                        || request.getServletPath().startsWith("/actuator"))
                .authorizeHttpRequests(auth -> auth.anyRequest().access((authentication, context) ->
                        new AuthorizationDecision(onManagementServer(context.getRequest()))))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
