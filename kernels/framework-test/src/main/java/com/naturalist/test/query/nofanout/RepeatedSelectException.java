package com.naturalist.test.query.nofanout;

/**
 * Thrown by {@link SelectTrace} when one call from a test read the same repository select
 * more than once — the N+1 fan-out this gate forbids. Extends
 * {@link AssertionError} so it fails the test that provoked it (raised from
 * {@code NaturalistTestExtension.afterEach}). The message names every offending
 * source, select and count, with a stack trace for each line that made the read. Fix the production fan-out to
 * batch the selects (see the package {@code CLAUDE.md}); do not catch this.
 */
public final class RepeatedSelectException extends AssertionError {
    public RepeatedSelectException(String message) {
        super(message);
    }
}
