package com.naturalist.usage;

import com.naturalist.ddd.Entity;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;

import java.time.Instant;
import java.util.function.Consumer;

/** Append-only identification event. Every event names the naturalist who caused it;
 *  global usage is count(all events), per-user usage is count(events for that naturalist). */
public record UsageEvent(UsageEventId id, UsageCounterName counterName,
                         NaturalistName naturalist, Instant instant) implements Entity<UsageEventId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(counterName, "counterName")
                .identifier(naturalist, "naturalist")
                .notNull(instant, "instant");
    }
}
