package com.naturalist.observability;

import java.util.Locale;

/**
 * Severity level attached to an emitted observation metric. Mirrors the
 * common log-severity vocabulary so dashboards and alerting can split on a
 * single tag rather than separate counters per severity.
 *
 * <h2>Picking a level at the call site</h2>
 * The framework decides the level only when an exception is thrown
 * ({@code throwWhenInvalid()} → {@link #ERROR}). Every other emission is
 * the call site's choice. Use the operator's question — <em>does seeing
 * this metric demand action, or is it just signal?</em> — to decide:
 *
 * <ul>
 *   <li>{@link #ERROR} — an exception was thrown. Reserved for the
 *       framework; call sites do not pass {@code ERROR} to
 *       {@code observe(Level)} (a fired metric without an accompanying
 *       throw is a contradiction).</li>
 *   <li>{@link #WARN} — the emission means <em>something is wrong</em>:
 *       invalid data, broken logic, a structural invariant the producer
 *       believed should hold. Producer self-observation of an assembled
 *       value typically uses {@code WARN} — the metric only fires when the
 *       producer's own output failed its invariants, which is either bad
 *       input or a bug. Operators should triage.</li>
 *   <li>{@link #INFO} — the emission is <em>signal, not problem</em>:
 *       a fact about the world the system can use to prioritise work, but
 *       which requires no immediate action. Forward-direction catalog
 *       search misses are the canonical case — the user typed a term the
 *       catalog does not yet know, which feeds the catalog's growth signal
 *       but is not itself a defect.</li>
 * </ul>
 *
 * <h2>Resolution rules</h2>
 * <ul>
 *   <li>{@code throwWhenInvalid()} emits violation counters at {@link #ERROR}
 *       — an exception is being thrown, the level is fixed.</li>
 *   <li>{@code observe(Level)} emits at the level the call site supplies —
 *       no exception is thrown, so the call site is the only place that
 *       knows whether the observation is informational or a warning.</li>
 * </ul>
 */
public enum Level {
    INFO,
    WARN,
    ERROR;

    /**
     * The Micrometer-friendly tag value for this level — lowercased
     * constant name. Used as the {@code level} tag on every emitted metric.
     */
    public String tagValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
