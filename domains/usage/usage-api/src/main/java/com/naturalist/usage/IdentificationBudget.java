package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;

/**
 * Write port consulted before an identification request is allowed to proceed.
 * <p>
 * {@link #reserve(NaturalistName)} increments the relevant usage counters and
 * throws {@link BudgetExceededException} if any configured limit (per-user
 * daily, global rate, global daily, or global monthly) would be exceeded. A
 * successful call reserves capacity; it does not need to be released.
 *
 * <p>The default {@link #noOp()} never throws and is suitable for unit tests
 * and composition roots that have not yet wired a production adapter.
 */
public interface IdentificationBudget {

    void reserve(NaturalistName naturalist);

    /**
     * A no-op {@code IdentificationBudget} that never rejects a reservation.
     */
    static IdentificationBudget noOp() {
        return NoOpIdentificationBudget.INSTANCE;
    }
}
