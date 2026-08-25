package com.naturalist.featuresearch;

import com.naturalist.ddd.EntityId;
import java.util.List;

/**
 * Finds existing features whose text is similar to a candidate value — a
 * per-domain, per-entity-id search seam. The in-memory {@link InMemoryFeatureSearch}
 * backs tests and the dev runtime; a production Solr/Postgres adapter implements the
 * same port later. Each domain wires its own instance over its own features; insect
 * and plant features never share a search.
 */
public interface FeatureSearch<ID extends EntityId> {

    /** Existing features similar to {@code value}, best first, at most {@code limit}. */
    List<FeatureMatch<ID>> findSimilar(String value, int limit);

    record FeatureMatch<ID extends EntityId>(ID id, String value, double score) {}
}
