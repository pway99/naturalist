package com.naturalist.insects;

import com.naturalist.taxonomy.RankAncestry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The insects wrapper over {@link RankAncestry}: the single place the insect FK chain
 * (species→genus→family→order) is encoded, as {@link #parentOf}. Shared by
 * {@link InsectCitationQueryImpl} and {@link InsectFeatureQueryImpl}.
 */
class InsectAncestryResolver {

    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectAncestryResolver(InsectQuery.SpeciesQuery speciesQuery,
                           InsectQuery.GenusQuery genusQuery,
                           InsectQuery.FamilyQuery familyQuery) {
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    /** The rank's Linnaean ancestry as an ancestor-first ordered set (order → … → subject).
     *  Ordered (LinkedHashSet) so it serves both a batched multi-rank query and ancestor-first
     *  grouping; a Set because the ancestry chain has no duplicate ranks. */
    Set<InsectRankName> ancestry(InsectRankName rankName) {
        List<InsectRankName> subjectFirst = RankAncestry.ancestry(rankName, this::parentOf); // subject-first
        LinkedHashSet<InsectRankName> ancestorFirst = new LinkedHashSet<>();
        for (int i = subjectFirst.size() - 1; i >= 0; i--) {
            ancestorFirst.add(subjectFirst.get(i));
        }
        return ancestorFirst;
    }

    /** Attributes across the ancestry tagged with source rank (blueprint C2). */
    <A> List<RankAncestry.AtRank<InsectRankName, A>> inherited(
            InsectRankName subject, Function<InsectRankName, List<A>> attributesAt) {
        return RankAncestry.inherited(subject, this::parentOf, attributesAt);
    }

    /** The insect FK chain: the parent rank of a given rank, empty at the order (or a gap). */
    private Optional<InsectRankName> parentOf(InsectRankName rankName) {
        return switch (rankName) {
            case InsectSpeciesName s -> speciesQuery.getByName(s).map(InsectSpecies::genusName);
            case InsectGenusName g -> genusQuery.getByName(g).map(InsectGenus::familyName);
            case InsectFamilyName f -> familyQuery.getByName(f).map(InsectFamily::orderName);
            case InsectOrderName o -> Optional.empty();
            case InsectSubspeciesName ss -> Optional.empty();
        };
    }
}
