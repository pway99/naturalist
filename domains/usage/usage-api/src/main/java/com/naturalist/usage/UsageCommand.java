package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;

import java.util.List;

/**
 * Write port over identification usage — the CQS command half of what was a single
 * combined read/write service. Symmetric write-side analogue of {@link UsageQuery}.
 *
 * <p>{@link #reserve(NaturalistName)} checks every active {@link UsageCounter} rule
 * on the {@code identification} counter, all-or-nothing: the first rule whose usage
 * would meet or exceed its limit rejects the whole reservation and inserts no
 * {@link UsageEvent}. A naturalist the injected {@code EntitlementLookup} entitles
 * bypasses these public-bucket rules entirely. The narrow {@link IdentificationBudget}
 * seam insects depends on forwards its single {@code reserve} method to this port.
 *
 * <p>{@link #reserve}, {@link #acknowledge}, and {@link #claimUnsentAlerts} are
 * {@code synchronized} on the adapter instance: the reserve algorithm is
 * check-then-insert across several repository round trips (including a gated
 * {@link UsageQuery#reserveState} call), so the synchronization boundary — not the
 * in-memory mock or a future RDBMS adapter — is what prevents two concurrent
 * callers from both observing {@code used == limit - 1} and both inserting past
 * the limit.
 */
public interface UsageCommand {

    void reserve(NaturalistName naturalist);

    void acknowledge(UsageAlertId id);

    /**
     * Marks every currently-unsent alert as {@code emailed=true} and returns the
     * alerts just claimed, so the caller can hand them to the mailer exactly once.
     */
    List<UsageAlert> claimUnsentAlerts();
}
