package com.naturalist.observability;

import com.naturalist.exception.InvariantViolationException;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The result of a single observation pass over a constraint graph. Holds the flattened
 * set of constraints, each with a fully-qualified dotted-path name, and offers two
 * terminal operations:
 *
 * <ul>
 *   <li>{@link #throwWhenInvalid()} — emit metrics and throw
 *       {@link InvariantViolationException} if any constraint fails. Violation
 *       metrics emitted from this path always carry {@code level=error} since
 *       an exception is about to be thrown.</li>
 *   <li>{@link #observe(Level)} — emit metrics only; no throw regardless of validity.
 *       The call site supplies the {@link Level} — only the call site knows
 *       whether the observation is informational ({@link Level#INFO}) or a
 *       warning ({@link Level#WARN}). Returns {@code this} so callers can inspect
 *       {@link #violations()} after.</li>
 * </ul>
 * <p>
 * Metric emission behaviour depends on the {@link Observer.MonitoringMode} supplied at
 * construction. With {@link Observer.MonitoringMode#ON_FAILURE}, only failing constraints
 * produce a metric. With {@link Observer.MonitoringMode#ALWAYS}, every inspected constraint
 * produces a metric — suitable for realtime traffic dashboards.
 *
 * <p>Every emitted metric carries a {@code level} tag — {@code error} when an
 * exception is being thrown, otherwise the call-site value passed to
 * {@link #observe(Level)}. ALWAYS-mode emissions from {@link #throwWhenInvalid()}
 * tag valid constraints {@code info} and invalid constraints {@code error} —
 * the validity of the individual constraint, not the validity of the batch.
 *
 * <p>Metrics are emitted via Micrometer's global {@code Metrics.globalRegistry} —
 * a {@code CompositeMeterRegistry} that delegates to all registered backends.
 * When no backends are configured it is effectively a no-op.
 */
public class InvariantObservation {

    private final String scope;
    private final Set<Constraint<?>> constraints;
    private final Observer.MonitoringMode monitoringMode;

    InvariantObservation(String scope, Set<Constraint<?>> constraints, Observer.MonitoringMode monitoringMode) {
        this.scope = scope;
        this.constraints = constraints == null ? Set.of() : Set.copyOf(constraints);
        this.monitoringMode = monitoringMode == null ? Observer.MonitoringMode.ON_FAILURE : monitoringMode;
    }

    /**
     * All constraints in this observation, valid and invalid.
     */
    public Set<Constraint<?>> constraints() {
        return constraints;
    }

    /**
     * Only the constraints that failed validation — invariant violations.
     */
    public Set<Constraint<?>> violations() {
        return constraints.stream()
                .filter(c -> !c.isValid())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> violationNames() {
        return violations().stream()
                .map(Constraint::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> violationNamesRemovingPrefix(String prefix) {
        return violations().stream()
                .map(Constraint::name)
                .map(n -> n.substring(prefix.length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Emit metrics and throw {@link InvariantViolationException} if any invariant is violated.
     * Violation metrics carry {@code level=error} — the throw is the trigger, the level
     * is fixed. With {@link Observer.MonitoringMode#ALWAYS}, an observation metric fires
     * for every constraint inspected regardless of validity, tagged per-constraint:
     * {@code level=error} for invalid, {@code level=info} for valid.
     */
    public void throwWhenInvalid() {
        Set<Constraint<?>> violated = violations();
        emitThrowingMetrics(violated);
        if (!violated.isEmpty()) {
            throw new InvariantViolationException(scope, violated);
        }
    }

    /**
     * Emit metrics without throwing. The {@code level} tag carries {@code level} on every
     * metric — only the call site knows whether this observation is informational or a
     * warning, so the call site supplies it. Returns {@code this} so callers can inspect
     * the observation after metric emission:
     *
     * <pre>{@code
     * InvariantObservation obs = observer.entity(e, "created").observe(Level.INFO);
     * if (!obs.violations().isEmpty()) { ... }
     * }</pre>
     */
    public InvariantObservation observe(Level level) {
        Level resolved = level == null ? Level.INFO : level;
        emitMetrics(violations(), resolved, resolved);
        return this;
    }

    public String scope() {
        return scope;
    }

    private void emitThrowingMetrics(Set<Constraint<?>> violated) {
        // throwWhenInvalid() always emits violations at ERROR — the throw is in flight.
        // ALWAYS-mode observations are tagged per-constraint validity, since an inspected
        // valid constraint is not the cause of any pending throw.
        emitMetrics(violated, Level.ERROR, Level.INFO);
    }

    private void emitMetrics(Set<Constraint<?>> violated, Level violationLevel, Level validObservationLevel) {
        try {
            if (monitoringMode == Observer.MonitoringMode.ALWAYS) {
                for (Constraint<?> c : constraints) {
                    Level perConstraint = c.isValid() ? validObservationLevel : violationLevel;
                    Metric.counter("naturalist.observation")
                            .tag("constraint", c.getClass().getSimpleName())
                            .tag("class", c.source())
                            .tag("method", c.methodName())
                            .tag("valid", c.isValid())
                            .tag("level", perConstraint.tagValue())
                            .incrementCounter();
                }
            }

            for (Constraint<?> c : violated) {
                Metric.counter("naturalist.invariant.violation")
                        .tag("constraint", c.getClass().getSimpleName())
                        .tag("class", c.source())
                        .tag("method", c.methodName())
                        .tag("level", violationLevel.tagValue())
                        .incrementCounter();
            }
        } catch (Exception ignored) {
            // The observability framework must never throw from metric emission.
        }
    }
}