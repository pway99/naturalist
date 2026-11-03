package com.naturalist.console.email;

import com.naturalist.notification.EmailSender;
import com.naturalist.notification.LoggingEmailSender;
import com.naturalist.notification.spring.SmtpEmailSender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The transport choice fails closed. No Spring context: the configuration method is called
 * directly with a real (empty or populated) bean-factory {@link ObjectProvider}, so the
 * question is only which sender comes back — or whether boot is refused — and what is observed.
 */
class EmailConfigurationTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final EmailConfiguration configuration = new EmailConfiguration();

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void noTransport_andNoLogOnlyOptIn_refusesToBoot_andIsObserved() {
        assertThatThrownBy(() -> configuration.emailSender(noMailSender(), properties(false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naturalist.email.log-only=true");

        assertThat(violations("EmailConfiguration.emailSender.transportConfigured")).isEqualTo(1);
        assertThat(violations("EmailConfiguration.emailSender.smtp")).isEqualTo(1);
    }

    @Test
    void noTransport_withLogOnlyOptIn_wiresTheLoggingSender_andKeepsTheSmtpViolationVisible() {
        EmailSender sender = configuration.emailSender(noMailSender(), properties(true));

        assertThat(sender).isInstanceOf(LoggingEmailSender.class);
        assertThat(violations("EmailConfiguration.emailSender.transportConfigured")).isZero();
        assertThat(violations("EmailConfiguration.emailSender.smtp")).isEqualTo(1);
    }

    @Test
    void mailSenderPresent_wiresSmtp_regardlessOfTheFlag() {
        EmailSender sender = configuration.emailSender(mailSender(), properties(false));

        assertThat(sender).isInstanceOf(SmtpEmailSender.class);
        assertThat(violations("EmailConfiguration.emailSender.transportConfigured")).isZero();
        assertThat(violations("EmailConfiguration.emailSender.smtp")).isZero();
    }

    private static EmailProperties properties(boolean logOnly) {
        return new EmailProperties("no-reply@example.com", "http://naturalist.test", logOnly);
    }

    private static ObjectProvider<MailSender> noMailSender() {
        return new DefaultListableBeanFactory().getBeanProvider(MailSender.class);
    }

    private static ObjectProvider<MailSender> mailSender() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("mailSender", new MailSender() {
            @Override
            public void send(SimpleMailMessage simpleMessage) {
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) {
            }
        });
        return factory.getBeanProvider(MailSender.class);
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }
}
