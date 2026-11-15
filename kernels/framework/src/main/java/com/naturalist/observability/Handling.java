package com.naturalist.observability;

import java.util.Locale;

/**
 * What a catch block did with the failure it caught, as reported to
 * {@link Observer#noticeError(String, String, Throwable, Handling)} and
 * {@link MethodObserver#noticeError(String, Throwable, Handling)}. It is the
 * {@code handling} tag on the {@code kind=caught} count, so a dashboard can
 * tell a failure the system absorbed from one that still reached a caller.
 */
public enum Handling {
    /** The catch carries on without the failure: it returns a fallback, skips the step, or does nothing. */
    SWALLOWED,
    /** The catch throws the same exception it caught, usually after cleaning up. */
    RETHROWN,
    /** The catch throws a new exception that carries the caught one as its cause. */
    WRAPPED;

    /**
     * The Micrometer-friendly tag value for this handling: the lowercased
     * constant name. Used as the {@code handling} tag on a caught-failure count.
     */
    public String tagValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
