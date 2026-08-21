package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

class InsectImageQueryImpl
        extends AbstractEntityQuery<
        InsectImageId,
        OrganismImage<InsectImageId, InsectObservationId, InsectRankName>,
        ImageCollection,
        InsectRepository.ImageRepository>
        implements InsectQuery.ImageQuery {

    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectImageQueryImpl(InsectRepository.ImageRepository repository,
                   InsectQuery.SpeciesQuery speciesQuery,
                   InsectQuery.GenusQuery genusQuery,
                   InsectQuery.FamilyQuery familyQuery) {
        super(repository);
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    @Override
    public ImageCollection findByNameSet(Set<InsectImageId> names) {
        observer().arguments("findByNameSet", i -> i
                        .identifierSet(names, "names"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public ImageCollection forParentName(InsectRankName parentName) {
        observer().arguments("forParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByParentName(parentName));
    }

    @Override
    public ImageCollection forParentNames(Set<InsectRankName> parentNames) {
        observer().arguments("forParentNames", i -> i.observableCollection(parentNames, "parentNames"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByParentNames(parentNames));
    }

    @Override
    public ImageCollection forRankHierarchy(InsectRankName rankName) {
        observer().arguments("forRankHierarchy", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByParentNames(subtreeRanks(rankName)));
    }

    /**
     * The full set of rank names in {@code rankName}'s subtree — the rank itself plus every
     * descendant rank down to species — resolved with one batched taxonomy query per rank
     * level (not one per node), so an order's subtree costs a handful of queries rather than
     * O(subtree size).
     */
    private Set<InsectRankName> subtreeRanks(InsectRankName rankName) {
        Set<InsectRankName> ranks = new LinkedHashSet<>();
        ranks.add(rankName);
        switch (rankName) {
            case InsectSpeciesName _ -> { /* species is the leaf — no descendants */ }
            case InsectSubspeciesName _ -> { /* no entity yet */ }
            case InsectGenusName genusName ->
                    speciesQuery.forGenusName(genusName).stream()
                            .map(InsectSpecies::name)
                            .forEach(ranks::add);
            case InsectFamilyName familyName -> {
                Set<InsectGenusName> genusNames = genusQuery.forFamilyName(familyName).stream()
                        .map(InsectGenus::name)
                        .collect(Collectors.toSet());
                ranks.addAll(genusNames);
                speciesQuery.forGenusNames(genusNames).stream()
                        .map(InsectSpecies::name)
                        .forEach(ranks::add);
            }
            case InsectOrderName orderName -> {
                Set<InsectFamilyName> familyNames = familyQuery.forOrderName(orderName).stream()
                        .map(InsectFamily::name)
                        .collect(Collectors.toSet());
                ranks.addAll(familyNames);
                Set<InsectGenusName> genusNames = genusQuery.forFamilyNames(familyNames).stream()
                        .map(InsectGenus::name)
                        .collect(Collectors.toSet());
                ranks.addAll(genusNames);
                speciesQuery.forGenusNames(genusNames).stream()
                        .map(InsectSpecies::name)
                        .forEach(ranks::add);
            }
        }
        return ranks;
    }
}