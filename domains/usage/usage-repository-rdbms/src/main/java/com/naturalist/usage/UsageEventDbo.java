package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link UsageEvent} — an append-only identification event, a surrogate-UUID
 * {@code Entity} with no foreign keys. {@code counterName} and {@code naturalist} are soft slugs.
 */
@DboSchema(table = "usage_event", primaryKey = "id", entity = UsageEvent.class)
final class UsageEventDbo implements Dbo {
    String id;
    String counterName;
    String naturalist;
    Instant instant;

    static UsageEventDbo from(UsageEvent e) {
        UsageEventDbo d = new UsageEventDbo();
        d.id = e.id().value().toString();
        d.counterName = e.counterName().value();
        d.naturalist = e.naturalist().value();
        d.instant = e.instant();
        Observer.forClass(UsageEventDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    UsageEvent toEntity() {
        return new UsageEvent(
                UsageEventId.of(UUID.fromString(id)),
                UsageCounterName.of(counterName),
                NaturalistName.of(naturalist),
                instant);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(counterName, "counterName").kebabFormat(counterName, "counterName")
                    .maxLength(counterName, 64, "counterName")
                .notNull(naturalist, "naturalist").kebabFormat(naturalist, "naturalist")
                    .maxLength(naturalist, 64, "naturalist")
                .notNull(instant, "instant");
    }
}
