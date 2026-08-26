package com.naturalist.console.usage;

import com.naturalist.usage.UsageMonitor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically claims unsent {@code UsageAlert}s and emails each (task D2).
 *
 * <p>{@link UsageMonitor#claimUnsentAlerts()} atomically marks the returned
 * alerts {@code emailed=true} in the same call that returns them, so a
 * second tick sees an empty list for anything already claimed here — the
 * "email once" guarantee lives entirely in that port, not in this job. A
 * failed {@link AlertEmailer#send} is swallowed by the emailer itself and
 * does not retry; the persisted alert row is the durable record (see
 * {@link AlertEmailer} class javadoc).
 */
@Component
class AlertDispatchJob {

    private final UsageMonitor monitor;
    private final AlertEmailer emailer;

    AlertDispatchJob(UsageMonitor monitor, AlertEmailer emailer) {
        this.monitor = monitor;
        this.emailer = emailer;
    }

    @Scheduled(fixedDelayString = "${naturalist.usage.dispatch-interval-ms:60000}")
    void dispatch() {
        for (var alert : monitor.claimUnsentAlerts()) {
            emailer.send(alert);
        }
    }
}
