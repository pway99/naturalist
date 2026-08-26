package com.naturalist.usage;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A raised threshold event for one {@link UsageCounter} over one period slug —
 * a warning or hard-stop crossing that a naturalist (or operator) may need to
 * see. Deduplicated per {@code (counter, scope, kind, period)}; {@code emailed}
 * and {@code acknowledged} track delivery and operator response.
 */
public record UsageAlert(
        UsageAlertId id,
        UsageCounterName counter,
        AlertScope scope,
        AlertKind kind,
        String period,
        String message,
        Instant at,
        boolean emailed,
        boolean acknowledged
) implements Entity<UsageAlertId> {

    public UsageAlert withEmailed(boolean newEmailed) {
        return new UsageAlert(id, counter, scope, kind, period, message, at, newEmailed, acknowledged);
    }

    public UsageAlert withAcknowledged(boolean newAcknowledged) {
        return new UsageAlert(id, counter, scope, kind, period, message, at, emailed, newAcknowledged);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(counter, "counter")
                .notNull(scope, "scope")
                .notNull(kind, "kind")
                .notBlank(period, "period")
                .notBlank(message, "message")
                .notNull(at, "at");
    }
}
