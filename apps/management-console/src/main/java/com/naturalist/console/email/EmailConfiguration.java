package com.naturalist.console.email;

import com.naturalist.notification.EmailSender;
import com.naturalist.notification.LoggingEmailSender;
import com.naturalist.notification.spring.SmtpEmailSender;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;

/**
 * Selects the outbound-email transport at the composition root: the SMTP adapter when a
 * {@link MailSender} bean is present (Spring Boot autoconfigures one from a populated
 * {@code spring.mail.*} block); otherwise the kernel's {@link LoggingEmailSender}, but
 * <strong>only</strong> when {@code naturalist.email.log-only=true} opts into it explicitly.
 * With neither, the application refuses to start — the same fail-closed stance as
 * {@code AdminProperties}. The log-only sender writes every verification and reset token to
 * the log beside the recipient's address; that is how dev and CI complete the auth flows with
 * no SMTP server, and it is an account-takeover primitive in a deployment whose operator
 * merely forgot the {@code spring.mail} block.
 *
 * <p>Both decisions are observed under {@code EmailConfiguration.emailSender}: a log-only
 * instance carries a standing {@code smtp} violation so it is visible on the dashboard.
 *
 * <p>Resolved once to the {@link EmailSender} port rather than guarded per-send: this is the
 * single place that knows about the transport, and every collaborator ({@code AlertEmailer},
 * the registration flow) depends only on the port.
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
class EmailConfiguration {

    private final Observer observer = Observer.forClass(EmailConfiguration.class, Observer.MonitoringMode.ALWAYS);

    @Bean
    EmailSender emailSender(ObjectProvider<MailSender> mailSender, EmailProperties properties) {
        MailSender transport = mailSender.getIfAvailable();
        boolean smtp = transport != null;
        boolean logOnly = properties.logOnly();
        MethodObserver emailSender = observer.forMethod("emailSender");
        // Both decisions are observed: a log-only deployment shows up as a standing `smtp`
        // violation on the dashboard; a deployment with neither trips `transportConfigured`.
        emailSender.state(i -> i
                .isTrue(smtp || logOnly, "transportConfigured")
                .isTrue(smtp, "smtp")).observe(Level.WARN);
        if (smtp) {
            return new SmtpEmailSender(transport, properties.from());
        }
        if (!logOnly) {
            // Fail closed, like AdminProperties: an unconfigured deployment must not fall back to
            // writing verification / reset tokens into its log.
            throw new IllegalStateException(
                    "no mail transport configured: set spring.mail.* for SMTP, or"
                            + " naturalist.email.log-only=true to opt into the log-only sender (dev / CI only)");
        }
        return new LoggingEmailSender();
    }
}
