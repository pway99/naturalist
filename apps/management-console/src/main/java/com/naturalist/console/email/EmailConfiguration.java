package com.naturalist.console.email;

import com.naturalist.notification.EmailSender;
import com.naturalist.notification.LoggingEmailSender;
import com.naturalist.notification.spring.SmtpEmailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;

/**
 * Selects the outbound-email transport at the composition root: the SMTP adapter when a
 * {@link MailSender} bean is present (Spring Boot autoconfigures one from a populated
 * {@code spring.mail.*} block), otherwise the kernel's {@link LoggingEmailSender} so every
 * environment — dev, CI, an unconfigured deploy — still completes the auth flows by logging
 * the link. Enabling SMTP is thus a config-only change.
 *
 * <p>Resolved once to the {@link EmailSender} port rather than guarded per-send: this is the
 * single place that knows about the transport, and every collaborator ({@code AlertEmailer},
 * the registration flow) depends only on the port.
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
class EmailConfiguration {

    @Bean
    EmailSender emailSender(ObjectProvider<MailSender> mailSender, EmailProperties properties) {
        MailSender transport = mailSender.getIfAvailable();
        return transport != null
                ? new SmtpEmailSender(transport, properties.from())
                : new LoggingEmailSender();
    }
}
