package com.naturalist.catalog;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.util.function.Consumer;

/**
 * Forward-direction miss observation — recorded whenever a search query
 * resolves to no hits.
 * <p>
 * Unlike {@link UnresolvedReferenceObservation}, an empty search result is
 * not a defect. It is the catalog's growth signal: aggregated over time,
 * the most-searched terms with no hits are the entities most worth adding
 * next. Observers therefore record this at INFO severity, not WARN, and
 * the metric tag is restricted to {@code query} alone (the only meaningful
 * dimension).
 *
 * @param query the (already-trimmed, normalised) search input that yielded
 *              no results
 */
public record UnresolvedSearchObservation(String query) implements Observable {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notBlank(query, "query");
    }
}
