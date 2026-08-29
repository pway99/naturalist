package com.naturalist.usage;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link UsageAlert} — a raised-threshold event, a surrogate-UUID {@code Entity}
 * with no foreign keys. {@code counter} is a soft activity slug. The dedup key
 * {@code (counter, scope, kind, period)} is a DDL-only composite UNIQUE.
 */
@DboSchema(table = "usage_alert", primaryKey = "id", entity = UsageAlert.class)
final class UsageAlertDbo implements Dbo {
    String id;
    String counter;
    String scope;
    String kind;
    String period;
    String message;
    Instant at;
    boolean emailed;
    boolean acknowledged;

    static UsageAlertDbo from(UsageAlert a) {
        UsageAlertDbo d = new UsageAlertDbo();
        d.id = a.id().value().toString();
        d.counter = a.counter().value();
        d.scope = a.scope().name();
        d.kind = a.kind().name();
        d.period = a.period();
        d.message = a.message();
        d.at = a.at();
        d.emailed = a.emailed();
        d.acknowledged = a.acknowledged();
        Observer.forClass(UsageAlertDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    UsageAlert toEntity() {
        return new UsageAlert(
                UsageAlertId.of(UUID.fromString(id)),
                UsageCounterName.of(counter),
                AlertScope.valueOf(scope),
                AlertKind.valueOf(kind),
                period,
                message,
                at,
                emailed,
                acknowledged);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(counter, "counter").kebabFormat(counter, "counter").maxLength(counter, 64, "counter")
                .notBlank(scope, "scope").maxLength(scope, 24, "scope")
                .notBlank(kind, "kind").maxLength(kind, 24, "kind")
                .notBlank(period, "period").maxLength(period, 64, "period")
                .notBlank(message, "message")
                .notNull(at, "at");
    }
}
