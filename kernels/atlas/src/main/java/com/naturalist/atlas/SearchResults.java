package com.naturalist.atlas;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The collection returned by {@link Atlas#search(String)} — a sequence of
 * {@link SearchHit}s, ordered by {@link MatchKind} and then by
 * {@link EntityRef} slug for determinism. Empty is a first-class state, not
 * an error.
 * <p>
 * Behavioral filters return new {@code SearchResults} instances via the
 * package-private constructor (per ADR-011). The empty state itself is a
 * deliberate signal — search misses are how the catalog learns what it
 * should grow to cover next.
 */
public final class SearchResults extends BehavioralCollection<SearchHit> {

    SearchResults(Collection<SearchHit> hits) {
        super(hits);
    }

    public static SearchResults of(Collection<SearchHit> hits) {
        return new SearchResults(hits);
    }

    public static SearchResults empty() {
        return new SearchResults(List.of());
    }

    /**
     * Group hits by the contributing domain, preserving the within-domain
     * ordering of this {@code SearchResults}. The returned map's iteration
     * order follows the order in which each domain first appears in the
     * underlying hit sequence.
     * <p>
     * Used by the search-results page to render one section per domain,
     * preserving the {@link MatchKind}-driven ranking inside each section.
     */
    public Map<DomainId, List<SearchHit>> groupedByDomain() {
        Map<DomainId, List<SearchHit>> grouped = new LinkedHashMap<>();
        stream().forEach(hit -> grouped
                .computeIfAbsent(hit.target().domain(), d -> new java.util.ArrayList<>())
                .add(hit));
        Map<DomainId, List<SearchHit>> immutable = new LinkedHashMap<>();
        grouped.forEach((domain, hits) -> immutable.put(domain, List.copyOf(hits)));
        return java.util.Collections.unmodifiableMap(immutable);
    }

    /**
     * Restrict the result set to hits of the given {@link MatchKind}. Useful
     * for a UI that wants to render only the strongest matches before
     * unfolding the rest.
     */
    public SearchResults byKind(MatchKind kind) {
        return new SearchResults(stream().filter(h -> h.kind() == kind).toList());
    }

    /**
     * Take the first {@code n} hits in their existing order. Returns
     * {@code this} if {@code n >= size()}; returns an empty
     * {@code SearchResults} for non-positive {@code n}.
     */
    public SearchResults topN(int n) {
        if (n <= 0) return empty();
        if (n >= size()) return this;
        return new SearchResults(stream().limit(n).toList());
    }
}
