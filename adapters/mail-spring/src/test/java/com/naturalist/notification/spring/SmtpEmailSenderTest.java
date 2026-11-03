package com.naturalist.notification.spring;

import com.naturalist.ddd.EmailAddress;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.notification.EmailMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SmtpEmailSender} against a hand-built {@link MailSender} fake — no
 * Spring context, no real SMTP. Covers the mapping from {@link EmailMessage} onto a
 * {@link SimpleMailMessage} (including the configured from-address) and the best-effort
 * contract: a transport failure is swallowed, not propagated.
 */
class SmtpEmailSenderTest {

    private static final EmailMessage MESSAGE = new EmailMessage(
            EmailAddress.of("naturalist@example.com"),
            "Verify your email",
            "Open http://localhost:8080/verify?token=abc to finish signing up.");

    @Test
    void mapsMessageOntoSimpleMailMessageWithConfiguredFrom() {
        RecordingMailSender transport = new RecordingMailSender();
        SmtpEmailSender sender = new SmtpEmailSender(transport, "no-reply@naturalist.local");

        sender.send(MESSAGE);

        assertThat(transport.sent).isNotNull();
        assertThat(transport.sent.getFrom()).isEqualTo("no-reply@naturalist.local");
        assertThat(transport.sent.getTo()).containsExactly("naturalist@example.com");
        assertThat(transport.sent.getSubject()).isEqualTo("Verify your email");
        assertThat(transport.sent.getText())
                .isEqualTo("Open http://localhost:8080/verify?token=abc to finish signing up.");
    }

    @Test
    void swallowsTransportFailure_butObservesIt() {
        SmtpEmailSender sender = new SmtpEmailSender(new FailingMailSender(), "no-reply@naturalist.local");
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
        try {
            assertThatCode(() -> sender.send(MESSAGE)).doesNotThrowAnyException();

            // Swallowed for the caller, visible to the operator.
            Counter undelivered = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                    .tag("name", "SmtpEmailSender.send.delivered").counter();
            assertThat(undelivered).isNotNull();
            assertThat(undelivered.count()).isEqualTo(1);
        } finally {
            Metrics.removeRegistry(meters);
        }
    }

    /** Captures the single {@link SimpleMailMessage} passed to {@code send}. */
    private static final class RecordingMailSender implements MailSender {
        private SimpleMailMessage sent;

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            this.sent = simpleMessage;
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            throw new UnsupportedOperationException("not used by SmtpEmailSender");
        }
    }

    /** Rejects every send, standing in for a misconfigured host or a bounced address. */
    private static final class FailingMailSender implements MailSender {
        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            throw new MailException("transport down") {};
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            throw new MailException("transport down") {};
        }
    }

    @Test
    void rejectsInvalidMessageBeforeReachingTransport() {
        RecordingMailSender transport = new RecordingMailSender();
        SmtpEmailSender sender = new SmtpEmailSender(transport, "no-reply@naturalist.local");
        EmailMessage blankRecipient = new EmailMessage(EmailAddress.of(" "), "Verify your email", "body");

        assertThatThrownBy(() -> sender.send(blankRecipient))
                .isInstanceOf(InvariantViolationException.class);
        assertThat(transport.sent).isNull();
    }
}
