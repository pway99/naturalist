package com.naturalist.console.usage;

import com.naturalist.usage.AlertKind;
import com.naturalist.usage.AlertScope;
import com.naturalist.usage.UsageAlert;
import com.naturalist.usage.UsageAlertId;
import com.naturalist.usage.UsageCounterName;
import com.naturalist.usage.UsageMonitor;
import com.naturalist.usage.UsageSnapshot;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test (no Spring context) for {@link AlertDispatchJob} — proves
 * the "email once" guarantee: {@link UsageMonitor#claimUnsentAlerts()}
 * returns the pending alert on the first call and an empty list on every
 * call after, so {@link AlertEmailer#send} must fire exactly once across
 * two {@code dispatch()} invocations regardless of how often the scheduler
 * ticks.
 */
class AlertDispatchJobTest {

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
    void emailsClaimedAlertExactlyOnceAcrossRepeatedDispatches() {
        ClaimOnceMonitor monitor = new ClaimOnceMonitor(List.of(ALERT));
        CountingMailSender sender = new CountingMailSender();
        AlertEmailer emailer = new AlertEmailer(
                providerReturning(sender),
                new UsageProperties(10, 3, 50, 650, 80, "alerts@example.com"));
        AlertDispatchJob job = new AlertDispatchJob(monitor, emailer);

        job.dispatch();
        job.dispatch();

        assertThat(sender.sendCount.get()).isEqualTo(1);
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

    /** Returns the given alerts once, then an empty list forever after — mirrors the atomic claim-and-mark contract of {@code UsageMonitor#claimUnsentAlerts()}. */
    private static final class ClaimOnceMonitor implements UsageMonitor {

        private List<UsageAlert> nextClaim;

        ClaimOnceMonitor(List<UsageAlert> firstClaim) {
            this.nextClaim = firstClaim;
        }

        @Override
        public UsageSnapshot snapshot() {
            throw new UnsupportedOperationException("not used by AlertDispatchJob");
        }

        @Override
        public List<UsageAlert> activeAlerts() {
            throw new UnsupportedOperationException("not used by AlertDispatchJob");
        }

        @Override
        public void acknowledge(UsageAlertId id) {
            throw new UnsupportedOperationException("not used by AlertDispatchJob");
        }

        @Override
        public List<UsageAlert> claimUnsentAlerts() {
            List<UsageAlert> claimed = nextClaim;
            nextClaim = List.of();
            return claimed;
        }
    }

    /** Counts {@code send(SimpleMailMessage)} invocations; the other {@link JavaMailSender} methods are unused by {@link AlertEmailer}. */
    private static final class CountingMailSender implements JavaMailSender {

        private final AtomicInteger sendCount = new AtomicInteger();

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            sendCount.incrementAndGet();
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
