package com.naturalist.usage;

import java.util.List;

/**
 * Read port over the current identification-usage picture: the aggregate
 * snapshot consumers render, and the alert lifecycle (list, acknowledge,
 * claim-for-email) operators and the notification adapter drive.
 */
public interface UsageMonitor {

    UsageSnapshot snapshot();

    /**
     * Unacknowledged alerts, newest first.
     */
    List<UsageAlert> activeAlerts();

    void acknowledge(UsageAlertId id);

    /**
     * Marks every currently-unsent alert as {@code emailed=true} and returns
     * the alerts just claimed, so the caller can hand them to the mailer
     * exactly once.
     */
    List<UsageAlert> claimUnsentAlerts();
}
