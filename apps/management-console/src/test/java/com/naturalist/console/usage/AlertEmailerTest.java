package com.naturalist.console.usage;

import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import com.naturalist.usage.AlertKind;
import com.naturalist.usage.AlertScope;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageCounterName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test (no Spring context) for {@link AlertEmailer}, now a thin translator onto
 * the {@link EmailSender} seam. Verifies the alert → {@link EmailMessage} mapping (recipient,
 * subject, body) against a recording sender; transport presence/absence and delivery-failure
 * handling are the port implementations' concern (covered in {@code SmtpEmailSenderTest} and
 * the kernel's {@code LoggingEmailSender}), no longer this class's.
 */
class AlertEmailerTest {

    private static final UsageProperties PROPERTIES =
            new UsageProperties(80, "alerts@example.com");

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
    void mapsAlertOntoEmailMessage() {
        RecordingEmailSender sender = new RecordingEmailSender();
        AlertEmailer emailer = new AlertEmailer(sender, PROPERTIES);

        emailer.send(ALERT);

        assertThat(sender.sent).isNotNull();
        assertThat(sender.sent.to()).isEqualTo("alerts@example.com");
        assertThat(sender.sent.subject()).isEqualTo("[naturalist] WARNING DAILY");
        assertThat(sender.sent.body()).isEqualTo("daily identification budget warning: 40/50 used");
    }

    /** Captures the single {@link EmailMessage} passed to {@code send}. */
    private static final class RecordingEmailSender implements EmailSender {
        private EmailMessage sent;

        @Override
        public void send(EmailMessage message) {
            this.sent = message;
        }
    }
}
