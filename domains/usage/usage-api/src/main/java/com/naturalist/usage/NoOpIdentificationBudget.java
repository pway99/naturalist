package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;

/**
 * Default {@link IdentificationBudget} that never rejects a reservation. The
 * intended use is unit tests and composition roots that have not yet wired a
 * production adapter — its existence is itself the signal, visible at the
 * call site ({@code IdentificationBudget.noOp()}).
 */
final class NoOpIdentificationBudget implements IdentificationBudget {

    static final NoOpIdentificationBudget INSTANCE = new NoOpIdentificationBudget();

    private NoOpIdentificationBudget() {
    }

    @Override
    public void reserve(NaturalistName naturalist) {
        // no-op
    }
}
