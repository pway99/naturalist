package com.naturalist.plants;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.taxonomy.RankAncestry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The plants wrapper over {@link RankAncestry}: the single place the plant FK chain
 * (species→genus→family→order) is encoded, as {@link #parentOf}. Mirrors
 * {@code InsectAncestryResolver}.
 */
@DomainService
class PlantAncestryResolver {

    private final PlantQuery.SpeciesQuery speciesQuery;
    private final PlantQuery.GenusQuery genusQuery;
    private final PlantQuery.FamilyQuery familyQuery;

    PlantAncestryResolver(PlantQuery.SpeciesQuery speciesQuery,
                          PlantQuery.GenusQuery genusQuery,
                          PlantQuery.FamilyQuery familyQuery) {
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    /** The rank's Linnaean ancestry as an ancestor-first ordered set (order → … → subject). */
    Set<PlantRankName> ancestry(PlantRankName rankName) {
        List<PlantRankName> subjectFirst = RankAncestry.ancestry(rankName, this::parentOf);
        LinkedHashSet<PlantRankName> ancestorFirst = new LinkedHashSet<>();
        for (int i = subjectFirst.size() - 1; i >= 0; i--) {
            ancestorFirst.add(subjectFirst.get(i));
        }
        return ancestorFirst;
    }

    /** The plant FK chain: the parent rank of a given rank, empty at the order (or a gap). */
    private Optional<PlantRankName> parentOf(PlantRankName rankName) {
        return switch (rankName) {
            case PlantSpeciesName s -> speciesQuery.getByName(s).map(PlantSpecies::genusName);
            case PlantGenusName g -> genusQuery.getByName(g).map(PlantGenus::familyName);
            case PlantFamilyName f -> familyQuery.getByName(f).map(PlantFamily::orderName);
            case PlantOrderName o -> Optional.empty();
        };
    }
}
