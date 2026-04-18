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
 *       {@link InvariantViolationException} if any constraint fails.</li>
 *   <li>{@link #observe()} — emit metrics only; no throw regardless of validity.
 *       Returns {@code this} so callers can inspect {@link #violations()} after.</li>
 * </ul>
 *
 * Metric emission behaviour depends on the {@link Observer.MonitoringMode} supplied at
 * construction. With {@link Observer.MonitoringMode#ON_FAILURE}, only failing constraints
 * produce a metric. With {@link Observer.MonitoringMode#ALWAYS}, every inspected constraint
 * produces a metric — suitable for realtime traffic dashboards.
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
     * With {@link Observer.MonitoringMode#ALWAYS}, a metric fires for every constraint
     * inspected regardless of validity.
     */
    public void throwWhenInvalid() {
        Set<Constraint<?>> violated = violations();
        emitMetrics(violated);
        if (!violated.isEmpty()) {
            throw new InvariantViolationException(scope, violated);
        }
    }

    /**
     * Emit metrics without throwing. Returns {@code this} so callers can inspect the
     * observation after metric emission:
     *
     * <pre>{@code
     * InvariantObservation obs = observer.entity(e, "created").observe();
     * if (!obs.violations().isEmpty()) { ... }
     * }</pre>
     */
    public InvariantObservation observe() {
        emitMetrics(violations());
        return this;
    }

    public String scope() {
        return scope;
    }

    private void emitMetrics(Set<Constraint<?>> violated) {
        try {
            if (monitoringMode == Observer.MonitoringMode.ALWAYS) {
                for (Constraint<?> c : constraints) {
                    Metric.counter("naturalist.observation")
                            .tag("constraint", c.getClass().getSimpleName())
                            .tag("class", c.source())
                            .tag("method", c.methodName())
                            .tag("valid", c.isValid())
                            .incrementCounter();
                }
            }

            for (Constraint<?> c : violated) {
                Metric.counter("naturalist.invariant.violation")
                        .tag("constraint", c.getClass().getSimpleName())
                        .tag("class", c.source())
                        .tag("method", c.methodName())
                        .incrementCounter();
            }
        } catch (Exception ignored) {
            // The observability framework must never throw from metric emission.
        }
    }
}