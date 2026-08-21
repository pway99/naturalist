package com.naturalist.plants;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Name-keyed, rank-polymorphic assembly of the {@link Plant} read model. Resolves the rank
 * chain from the given {@link PlantRankName} upward to the order. Chunk 1 composes only the
 * ancestry spine; base attributes (images, features, role) and children fold in over later
 * chunks. Mirrors {@code InsectFactory}.
 */
class PlantFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantQuery.SpeciesQuery speciesQuery;
    private final PlantQuery.GenusQuery genusQuery;
    private final PlantQuery.FamilyQuery familyQuery;
    private final PlantQuery.OrderQuery orderQuery;
    private final PlantQuery.FeatureQuery featureQuery;

    PlantFactory(PlantQuery.SpeciesQuery speciesQuery,
                 PlantQuery.GenusQuery genusQuery,
                 PlantQuery.FamilyQuery familyQuery,
                 PlantQuery.OrderQuery orderQuery,
                 PlantQuery.FeatureQuery featureQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(featureQuery, "featureQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
        this.orderQuery = orderQuery;
        this.featureQuery = featureQuery;
    }

    Optional<Plant> buildByName(PlantRankName name) {
        observer.arguments("buildByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return switch (name) {
            case PlantSpeciesName sn -> speciesQuery.getByName(sn).map(s -> observe(
                    resolveGenus(base(sn).withSpecies(PlantSpeciesView.of(s)).withChildren(java.util.List.of()),
                            s.genusName())));
            case PlantGenusName gn -> genusQuery.getByName(gn).map(g -> observe(
                    resolveFamily(base(gn).withGenus(PlantGenusView.of(g)).withChildren(speciesChildren(gn)),
                            g.familyName())));
            case PlantFamilyName fn -> familyQuery.getByName(fn).map(f -> observe(
                    resolveOrder(base(fn).withFamily(PlantFamilyView.of(f)).withChildren(genusChildren(fn)),
                            f.orderName())));
            case PlantOrderName on -> orderQuery.getByName(on).map(o -> observe(
                    base(on).withOrder(PlantOrderView.of(o)).withChildren(familyChildren(on))));
        };
    }

    private Plant base(PlantRankName name) {
        return Plant.empty().withFeatures(featureQuery.findByRankName(name));
    }

    private java.util.List<PlantTaxonView> familyChildren(PlantOrderName orderName) {
        return familyQuery.forOrderName(orderName).stream()
                .map(f -> (PlantTaxonView) PlantFamilyView.of(f))
                .sorted(java.util.Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private java.util.List<PlantTaxonView> genusChildren(PlantFamilyName familyName) {
        return genusQuery.forFamilyName(familyName).stream()
                .map(g -> (PlantTaxonView) PlantGenusView.of(g))
                .sorted(java.util.Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private java.util.List<PlantTaxonView> speciesChildren(PlantGenusName genusName) {
        return speciesQuery.forGenusName(genusName).stream()
                .map(s -> (PlantTaxonView) PlantSpeciesView.of(s))
                .sorted(java.util.Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private Plant resolveGenus(Plant plant, PlantGenusName genusName) {
        return genusQuery.getByName(genusName)
                .map(genus -> resolveFamily(plant.withGenus(PlantGenusView.of(genus)), genus.familyName()))
                .orElse(plant);
    }

    private Plant resolveFamily(Plant plant, PlantFamilyName familyName) {
        return familyQuery.getByName(familyName)
                .map(family -> resolveOrder(plant.withFamily(PlantFamilyView.of(family)), family.orderName()))
                .orElse(plant);
    }

    private Plant resolveOrder(Plant plant, PlantOrderName orderName) {
        return orderQuery.getByName(orderName)
                .map(order -> plant.withOrder(PlantOrderView.of(order)))
                .orElse(plant);
    }

    private Plant observe(Plant plant) {
        observer.observable(plant, "plant").observe(Level.WARN);
        return plant;
    }
}
