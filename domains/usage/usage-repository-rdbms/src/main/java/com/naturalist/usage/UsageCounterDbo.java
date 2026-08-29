package com.naturalist.usage;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link UsageCounter} — an editable budget rule, a surrogate-UUID {@code Entity}
 * with no owned children and no foreign keys. {@code counterName} is a soft activity slug. The logical key
 * {@code (counter_name, scope, window_kind)} is a DDL-only composite UNIQUE. {@code limit} is a reserved
 * word, so the column is {@code limit_value}.
 */
@DboSchema(table = "usage_counter", primaryKey = "id", entity = UsageCounter.class)
final class UsageCounterDbo implements Dbo {
    String id;
    String counterName;
    String scope;
    String windowKind;
    Instant since;      // nullable
    int limitValue;
    boolean active;

    static UsageCounterDbo from(UsageCounter c) {
        UsageCounterDbo d = new UsageCounterDbo();
        d.id = c.id().value().toString();
        d.counterName = c.counterName().value();
        d.scope = c.scope().name();
        d.windowKind = c.windowKind().name();
        d.since = c.since();
        d.limitValue = c.limit();
        d.active = c.active();
        Observer.forClass(UsageCounterDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    UsageCounter toEntity() {
        return new UsageCounter(
                UsageCounterId.of(UUID.fromString(id)),
                UsageCounterName.of(counterName),
                UsageScope.valueOf(scope),
                WindowKind.valueOf(windowKind),
                since,
                limitValue,
                active);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(counterName, "counterName").kebabFormat(counterName, "counterName")
                    .maxLength(counterName, 64, "counterName")
                .notBlank(scope, "scope").maxLength(scope, 24, "scope")
                .notBlank(windowKind, "windowKind").maxLength(windowKind, 24, "windowKind");
    }
}
