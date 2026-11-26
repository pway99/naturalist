package com.naturalist.console.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * The session registry behind {@link AccountSessions}. {@link HttpSessionEventPublisher} is a
 * servlet {@code HttpSessionListener} (Boot registers it with the container) that turns session
 * destruction and id changes into application events, which {@link SessionRegistryImpl} listens
 * to: without it, sessions that time out or log out would stay in the registry for ever, and a
 * rotated session id would leave the registry pointing at the old one.
 */
@Configuration
class SessionConfiguration {

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
