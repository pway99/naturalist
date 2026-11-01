package com.naturalist.console;

import com.naturalist.console.admin.AdminProperties;
import com.naturalist.console.auth.LoginRateLimitFilter;
import com.naturalist.resilience.Resilience;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AdminProperties.class)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, Resilience resilience) throws Exception {
        return http
                // Global ceiling on POST /login (auth.login): caps the BCrypt CPU a
                // credential-stuffing run can burn; rejections are observed as withinRate.
                .addFilterBefore(new LoginRateLimitFilter(resilience), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Static assets + entry / self-service auth pages — open to everyone.
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**",
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
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
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
}
