package com.naturalist.usage;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A named bucket of identification-usage tallies and alerts — the natural-key
 * anchor for {@link UsageTally} and {@link UsageAlert} records. The single
 * seeded counter is {@code identification}.
 */
public record UsageCounter(UsageCounterName name) implements NamedEntity<UsageCounterName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.identifier(name, "name");
    }
}
