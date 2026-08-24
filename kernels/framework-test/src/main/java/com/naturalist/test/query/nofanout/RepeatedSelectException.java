package com.naturalist.test.query.nofanout;

/**
 * Thrown by {@link SelectGate} when a single head-of-DAG query invocation resolved a
 * repository select more than once — the N+1 fan-out this gate forbids. Extends
 * {@link AssertionError} so it fails the test that provoked it (raised from
 * {@code NaturalistTestExtension.afterEach}). The message names every offending
 * {@code head query} + {@code repository select} + count. Fix the production fan-out to
 * batch the selects (see the package {@code CLAUDE.md}); do not catch this.
 */
public final class RepeatedSelectException extends AssertionError {
    public RepeatedSelectException(String message) {
        super(message);
    }
}
