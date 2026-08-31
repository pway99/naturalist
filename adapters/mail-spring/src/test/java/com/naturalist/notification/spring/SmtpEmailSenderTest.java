package com.naturalist.notification.spring;

import com.naturalist.notification.EmailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link SmtpEmailSender} against a hand-built {@link MailSender} fake — no
 * Spring context, no real SMTP. Covers the mapping from {@link EmailMessage} onto a
 * {@link SimpleMailMessage} (including the configured from-address) and the best-effort
 * contract: a transport failure is swallowed, not propagated.
 */
class SmtpEmailSenderTest {

    private static final EmailMessage MESSAGE = new EmailMessage(
            "naturalist@example.com",
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
    void swallowsTransportFailure() {
        SmtpEmailSender sender = new SmtpEmailSender(new FailingMailSender(), "no-reply@naturalist.local");

        assertThatCode(() -> sender.send(MESSAGE)).doesNotThrowAnyException();
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
}
