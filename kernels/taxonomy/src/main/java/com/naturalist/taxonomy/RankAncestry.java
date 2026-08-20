package com.naturalist.taxonomy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Generic, rank-agnostic cross-rank ancestry resolution (organism-domain blueprint C2).
 * The walk is data-driven through a domain-supplied {@code parentOf}; this class names
 * no taxonomic rung and does no {@link LinealRank} arithmetic, so each domain's ladder
 * (four rungs, five, or otherwise — with or without subspecies) is expressed entirely
 * by its own {@code parentOf} over its own sealed rank names.
 */
public final class RankAncestry {

    private RankAncestry() {}

    /**
     * The ancestry chain from {@code subject} upward: subject first, then ancestors in
     * ascending order. Stops when {@code parentOf} yields empty (top rank, or a missing
     * link — the chain ends at the gap). Cycle-guarded: a rank that reappears terminates
     * the walk rather than looping.
     */
    public static <R extends RankName> List<R> ancestry(R subject, Function<R, Optional<R>> parentOf) {
        List<R> chain = new ArrayList<>();
        Set<R> seen = new HashSet<>();
        R current = subject;
        while (current != null && seen.add(current)) {
            chain.add(current);
            current = parentOf.apply(current).orElse(null);
        }
        return List.copyOf(chain);
    }

    /**
     * Attributes attached across the ancestry, each tagged with the rank it was attached
     * at — the lineage composite. Returned in ancestry order (subject first); a consumer
     * that wants ancestor-first reverses the result. Ranks contributing no attributes
     * simply do not appear.
     */
    public static <R extends RankName, A> List<AtRank<R, A>> inherited(
            R subject, Function<R, Optional<R>> parentOf, Function<R, List<A>> attributesAt) {
        List<AtRank<R, A>> result = new ArrayList<>();
        for (R rank : ancestry(subject, parentOf)) {
            for (A value : attributesAt.apply(rank)) {
                result.add(new AtRank<>(value, rank));
            }
        }
        return List.copyOf(result);
    }

    /** An attribute tagged with the ancestry rank it was attached at. */
    public record AtRank<R extends RankName, A>(A value, R sourceRank) {}
}
