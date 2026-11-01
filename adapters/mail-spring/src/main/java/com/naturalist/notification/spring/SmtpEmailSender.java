package com.naturalist.notification.spring;

import com.naturalist.notification.EmailMessage;
import com.naturalist.notification.EmailSender;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * {@link EmailSender} over Spring's {@link MailSender} (SMTP transport). An invalid message
 * is rejected up front with an {@code InvariantViolationException}; a transport failure on
 * a valid one is logged and swallowed, never propagated — the caller has already committed
 * the durable record (a persisted token, an alert row), and a bounced send must not fail
 * its request.
 *
 * <p>Swallowed is not unobserved: every send is observed as the predicate {@code delivered}
 * under {@link Observer.MonitoringMode#ALWAYS}, so a dead or misconfigured transport shows
 * up as a {@code SmtpEmailSender.send.delivered} violation rate — and the valid rows are the
 * delivery baseline — rather than as a log line nobody is reading while registrations
 * silently dead-end.
 *
 * <p>Constructed at the app composition root from the {@code JavaMailSender} bean Spring
 * Boot autoconfigures when {@code spring.mail.*} is populated; when it is not, the app wires
 * the kernel's {@code LoggingEmailSender} instead. Enabling SMTP in production is therefore
 * a config-only change — no code path here changes.
 *
 * <p>Depends only on {@code MailSender} / {@link SimpleMailMessage} (plain-text send), not
 * the {@code javamail} subpackage, so the adapter compiles without a {@code jakarta.mail}
 * dependency; the concrete {@code JavaMailSender} the app injects is a {@code MailSender}.
 */
public final class SmtpEmailSender implements EmailSender {

    private static final Logger LOG = System.getLogger(SmtpEmailSender.class.getName());

    private final Observer observer = Observer.forClass(getClass(), Observer.MonitoringMode.ALWAYS);
    private final MailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSender(MailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(EmailMessage message) {
        MethodObserver send = observer.forMethod("send");
        send.arguments(i -> i.valueObject(message, "message")).throwWhenInvalid();
        var mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(message.to().value());
        mail.setSubject(message.subject());
        mail.setText(message.body());

        boolean delivered = deliver(mail, message);
        send.arguments(i -> i.isTrue(delivered, "delivered"))
                .observe(com.naturalist.observability.Level.WARN);
    }

    /** Best-effort transport call: {@code false} on a swallowed {@link MailException}. */
    private boolean deliver(SimpleMailMessage mail, EmailMessage message) {
        try {
            mailSender.send(mail);
            return true;
        } catch (MailException transportFailure) {
            LOG.log(Level.WARNING,
                    "[email:smtp] delivery to {0} failed; swallowed (best-effort). subject: {1}",
                    message.to().value(), message.subject());
            return false;
        }
    }
}
