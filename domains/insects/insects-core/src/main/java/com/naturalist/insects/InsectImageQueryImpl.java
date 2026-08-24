package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.InsectEntityCollections.ImageGallery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@DomainService
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

    @Override
    public ImageGallery forRankHierarchies(Set<InsectRankName> rankNames) {
        observer().arguments("forRankHierarchies", i -> i.observableCollection(rankNames, "rankNames"))
                .throwWhenInvalid();

        // Attribute every rank in any root's subtree back to the root it descends from, expanding
        // the whole forest one batched taxonomy query per level (not one subtree walk per root).
        Map<InsectRankName, InsectRankName> rootOf = new LinkedHashMap<>();
        for (InsectRankName root : rankNames) {
            rootOf.put(root, root);
        }

        Set<InsectOrderName> orderRoots = ofType(rankNames, InsectOrderName.class);
        if (!orderRoots.isEmpty()) {
            for (InsectFamily family : familyQuery.forOrderNames(orderRoots).stream().toList()) {
                rootOf.putIfAbsent(family.name(), rootOf.get(family.orderName()));
            }
        }

        Set<InsectFamilyName> familyNames = ofType(rootOf.keySet(), InsectFamilyName.class);
        if (!familyNames.isEmpty()) {
            for (InsectGenus genus : genusQuery.forFamilyNames(familyNames).stream().toList()) {
                InsectRankName root = rootOf.get(genus.familyName());
                if (root != null) {
                    rootOf.putIfAbsent(genus.name(), root);
                }
            }
        }

        Set<InsectGenusName> genusNames = ofType(rootOf.keySet(), InsectGenusName.class);
        if (!genusNames.isEmpty()) {
            for (InsectSpecies species : speciesQuery.forGenusNames(genusNames).stream().toList()) {
                InsectRankName root = rootOf.get(species.genusName());
                if (root != null) {
                    rootOf.putIfAbsent(species.name(), root);
                }
            }
        }

        // Every requested root is a key (empty when it has no subtree images); one image fetch
        // across the whole forest, each image routed to its root's bucket.
        Map<InsectRankName, Collection<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>>> byRoot =
                new LinkedHashMap<>();
        for (InsectRankName root : rankNames) {
            byRoot.put(root, new ArrayList<>());
        }
        if (!rootOf.isEmpty()) {
            for (OrganismImage<InsectImageId, InsectObservationId, InsectRankName> image
                    : repository().getByParentNames(new LinkedHashSet<>(rootOf.keySet()))) {
                InsectRankName root = rootOf.get(image.parentName());
                if (root != null) {
                    byRoot.get(root).add(image);
                }
            }
        }
        return ImageGallery.grouped(byRoot);
    }

    private static <T extends InsectRankName> Set<T> ofType(Set<InsectRankName> ranks, Class<T> type) {
        return ranks.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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