package com.naturalist.observability;

import java.util.Locale;

/**
 * What an {@link InvariantObservation} looked at, carried as the {@code kind} tag on every
 * {@code naturalist.invariant.check} emission so dashboards and alerts can tell a bad caller
 * from a bad dependency without a second meter.
 *
 * <ul>
 *   <li>{@link #ARGUMENT} — values the caller passed in, from
 *       {@link Observer#arguments(String, java.util.function.Consumer)}. A violation points at
 *       the caller.</li>
 *   <li>{@link #STATE} — values the method acquired or computed during its own run: an
 *       aggregate fetched from another api, a count, a looked-up mapping. From
 *       {@link Observer#state(String, java.util.function.Consumer)}. A violation points at
 *       what the method consumed or derived, not at its caller.</li>
 *   <li>{@link #OBSERVABLE} — an {@link Observable}'s own declared {@code invariants()}, from
 *       {@link Observer#observable(Observable, String)}, {@link Observer#namedEntity}, and
 *       {@link Observer#observation(Observable)}.</li>
 * </ul>
 */
public enum ObservationKind {
    ARGUMENT,
    STATE,
    OBSERVABLE;

    /**
     * The Micrometer-friendly tag value for this kind — lowercased constant name. Used as the
     * {@code kind} tag on every emitted metric.
     */
    public String tagValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
