package com.naturalist.persistence;

import com.naturalist.observability.Observable;

/**
 * Marker for a database object: a plain, snake_case, auto-mapped persistence POJO that
 * declares its own {@code invariants()} (column widths, {@code NOT NULL}s).
 *
 * <p>A DBO is built from an entity only in order to persist it, so each concrete DBO's
 * {@code from(entity)} factory observes the DBO through an {@code Observer} scoped to that
 * factory ({@code <Dbo>.from}) and throws {@code InvariantViolationException} before
 * returning — a value that would violate a column constraint fails at construction, one step
 * before the database, and the violation is tagged with its originating factory. The
 * observer is invoked inline in {@code from(...)} rather than via a marker-interface default
 * so the observation carries the real method name, not a generic one.
 */
public interface Dbo extends Observable {
}
