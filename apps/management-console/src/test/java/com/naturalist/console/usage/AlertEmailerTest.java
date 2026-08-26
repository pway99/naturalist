package com.naturalist.console.usage;

import com.naturalist.usage.AlertKind;
import com.naturalist.usage.AlertScope;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageCounterName;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Plain unit tests (no Spring context) for {@link AlertEmailer} — the
 * guarded, best-effort email sender for {@link UsageAlert}s. Both the
 * present-sender and absent-sender paths are exercised via a hand-built
 * {@link ObjectProvider} fake, per the task brief.
 */
class AlertEmailerTest {

    private static final UsageProperties PROPERTIES =
            new UsageProperties(10, 3, 50, 650, 80, "alerts@example.com");

    private static final UsageAlert ALERT = new UsageAlert(
            UsageAlertId.create(),
            UsageCounterName.of("identification"),
            AlertScope.DAILY,
            AlertKind.WARNING,
            "daily-2026-08-25",
            "daily identification budget warning: 40/50 used",
            Instant.parse("2026-08-25T15:00:00Z"),
            false,
            false);

    @Test
    void sendsSimpleMailMessageWhenMailSenderConfigured() {
        RecordingMailSender sender = new RecordingMailSender();
        AlertEmailer emailer = new AlertEmailer(providerReturning(sender), PROPERTIES);

        emailer.send(ALERT);

        assertThat(sender.sent).isNotNull();
        assertThat(sender.sent.getTo()).containsExactly("alerts@example.com");
        assertThat(sender.sent.getSubject()).isEqualTo("[naturalist] WARNING DAILY");
        assertThat(sender.sent.getText()).isEqualTo("daily identification budget warning: 40/50 used");
    }

    @Test
    void noOpsWhenNoMailSenderIsConfigured() {
        AlertEmailer emailer = new AlertEmailer(providerReturning(null), PROPERTIES);

        assertThatCode(() -> emailer.send(ALERT)).doesNotThrowAnyException();
    }

    private static ObjectProvider<JavaMailSender> providerReturning(JavaMailSender sender) {
        return new ObjectProvider<>() {
            @Override
            public JavaMailSender getObject() throws BeansException {
                return sender;
            }

            @Override
            public JavaMailSender getIfAvailable() throws BeansException {
                return sender;
            }
        };
    }

    /** Captures the single {@link SimpleMailMessage} passed to {@code send}. */
    private static final class RecordingMailSender implements JavaMailSender {

        private SimpleMailMessage sent;

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            this.sent = simpleMessage;
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            throw new UnsupportedOperationException("not used by AlertEmailer");
        }

        @Override
        public MimeMessage createMimeMessage() {
            throw new UnsupportedOperationException("not used by AlertEmailer");
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
            throw new UnsupportedOperationException("not used by AlertEmailer");
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            throw new UnsupportedOperationException("not used by AlertEmailer");
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            throw new UnsupportedOperationException("not used by AlertEmailer");
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
            throw new UnsupportedOperationException("not used by AlertEmailer");
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
            throw new UnsupportedOperationException("not used by AlertEmailer");
        }
    }
}
