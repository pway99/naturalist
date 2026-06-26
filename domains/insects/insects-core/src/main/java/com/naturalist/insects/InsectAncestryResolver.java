package com.naturalist.insects;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the Linnaean ancestry chain for an {@link InsectRankName} — the subject
 * rank followed by its ancestors up to the order.
 * <p>
 * Shared by {@link InsectCitationQueryImpl} (citations) and
 * {@link InsectFeatureQueryImpl} (features) — both need "everything at this rank
 * plus everything inherited from ancestors, tagged with provenance." The traversal
 * is the reuse; the types are distinct.
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

    /**
     * Returns the ancestry chain starting from the given rank and walking upward
     * to the order. The subject rank is always first; ancestors follow in ascending
     * Linnaean order (species → genus → family → order).
     */
    List<InsectRankName> resolveAncestry(InsectRankName rankName) {
        List<InsectRankName> ancestry = new ArrayList<>();
        ancestry.add(rankName);

        return switch (rankName) {
            case InsectSpeciesName speciesName -> {
                speciesQuery.getByName(speciesName).ifPresent(species -> {
                    ancestry.add(species.genusName());
                    genusQuery.getByName(species.genusName()).ifPresent(genus -> {
                        ancestry.add(genus.familyName());
                        familyQuery.getByName(genus.familyName()).ifPresent(family ->
                                ancestry.add(family.orderName()));
                    });
                });
                yield ancestry;
            }
            case InsectGenusName genusName -> {
                genusQuery.getByName(genusName).ifPresent(genus -> {
                    ancestry.add(genus.familyName());
                    familyQuery.getByName(genus.familyName()).ifPresent(family ->
                            ancestry.add(family.orderName()));
                });
                yield ancestry;
            }
            case InsectFamilyName familyName -> {
                familyQuery.getByName(familyName).ifPresent(family ->
                        ancestry.add(family.orderName()));
                yield ancestry;
            }
            case InsectOrderName _ -> ancestry;
            case InsectSubspeciesName _ -> ancestry;
        };
    }
}