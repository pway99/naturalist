package com.naturalist.persistence;

import com.naturalist.observability.Observable;

/** Marker for a database object: a plain, snake_case, auto-mapped persistence POJO that
 *  declares its own {@code invariants()} so the adapter can validate it at the ACL boundary. */
public interface Dbo extends Observable {
}
