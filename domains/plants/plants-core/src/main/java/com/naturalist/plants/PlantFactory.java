package com.naturalist.plants;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.plants.cultivar.CultivarQuery;
import com.naturalist.plants.management.PlantProgramQuery;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Name-keyed, rank-polymorphic assembly of the {@link Plant} read model. Resolves the rank
 * chain from the given {@link PlantRankName} upward to the order. Mirrors {@code InsectFactory}.
 *
 * <p>The ancestor rank chain is resolved <em>once</em> per build — each ancestor entity is
 * fetched a single time and its {@link PlantRankName} folded into the lineage set that
 * {@link PlantFeatureQueryImpl#findByAncestry} consumes. Before this, the feature query and
 * this factory each walked the chain independently, so every ancestor's {@code getByName}
 * fired twice per page — the N+1 the runtime select gate flags.
 */
class PlantFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final PlantQuery.SpeciesQuery speciesQuery;
    private final PlantQuery.GenusQuery genusQuery;
    private final PlantQuery.FamilyQuery familyQuery;
    private final PlantQuery.OrderQuery orderQuery;
    private final PlantFeatureQueryImpl featureQuery;
    private final PlantQuery.EcologicalRoleQuery roleQuery;
    private final PlantQuery.ImageQuery imageQuery;
    private final CultivarQuery cultivarQuery;
    private final PlantProgramQuery programQuery;
    private final PhytochemicalConstituentQuery constituentQuery;

    PlantFactory(PlantQuery.SpeciesQuery speciesQuery,
                 PlantQuery.GenusQuery genusQuery,
                 PlantQuery.FamilyQuery familyQuery,
                 PlantQuery.OrderQuery orderQuery,
                 PlantFeatureQueryImpl featureQuery,
                 PlantQuery.EcologicalRoleQuery roleQuery,
                 PlantQuery.ImageQuery imageQuery,
                 CultivarQuery cultivarQuery,
                 PlantProgramQuery programQuery,
                 PhytochemicalConstituentQuery constituentQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(featureQuery, "featureQuery")
                        .notNull(roleQuery, "roleQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(cultivarQuery, "cultivarQuery")
                        .notNull(programQuery, "programQuery")
                        .notNull(constituentQuery, "constituentQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
        this.orderQuery = orderQuery;
        this.featureQuery = featureQuery;
        this.roleQuery = roleQuery;
        this.imageQuery = imageQuery;
        this.cultivarQuery = cultivarQuery;
        this.programQuery = programQuery;
        this.constituentQuery = constituentQuery;
    }

    Optional<Plant> buildByName(PlantRankName name) {
        observer.arguments("buildByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return switch (name) {
            case PlantSpeciesName sn -> speciesQuery.getByName(sn).map(species -> {
                Optional<PlantGenus> genus = genusQuery.getByName(species.genusName());
                Optional<PlantFamily> family = genus.flatMap(g -> familyQuery.getByName(g.familyName()));
                Optional<PlantOrder> order = family.flatMap(f -> orderQuery.getByName(f.orderName()));
                Set<PlantRankName> ancestry = lineage(sn,
                        genus.map(PlantGenus::name), family.map(PlantFamily::name), order.map(PlantOrder::name));
                Plant plant = base(sn, ancestry)
                        .withSpecies(PlantSpeciesView.of(species))
                        .withChildren(List.of())
                        .withCultivars(cultivarQuery.forPlantName(sn))
                        .withPrograms(programQuery.forPlantName(sn))
                        .withConstituents(constituentQuery.forPlantName(sn));
                if (genus.isPresent()) plant = plant.withGenus(PlantGenusView.of(genus.get()));
                if (family.isPresent()) plant = plant.withFamily(PlantFamilyView.of(family.get()));
                if (order.isPresent()) plant = plant.withOrder(PlantOrderView.of(order.get()));
                return observe(plant);
            });
            case PlantGenusName gn -> genusQuery.getByName(gn).map(genus -> {
                Optional<PlantFamily> family = familyQuery.getByName(genus.familyName());
                Optional<PlantOrder> order = family.flatMap(f -> orderQuery.getByName(f.orderName()));
                Set<PlantRankName> ancestry = lineage(gn,
                        family.map(PlantFamily::name), order.map(PlantOrder::name));
                Plant plant = base(gn, ancestry)
                        .withGenus(PlantGenusView.of(genus))
                        .withChildren(speciesChildren(gn));
                if (family.isPresent()) plant = plant.withFamily(PlantFamilyView.of(family.get()));
                if (order.isPresent()) plant = plant.withOrder(PlantOrderView.of(order.get()));
                return observe(plant);
            });
            case PlantFamilyName fn -> familyQuery.getByName(fn).map(family -> {
                Optional<PlantOrder> order = orderQuery.getByName(family.orderName());
                Set<PlantRankName> ancestry = lineage(fn, order.map(PlantOrder::name));
                Plant plant = base(fn, ancestry)
                        .withFamily(PlantFamilyView.of(family))
                        .withChildren(genusChildren(fn));
                if (order.isPresent()) plant = plant.withOrder(PlantOrderView.of(order.get()));
                return observe(plant);
            });
            case PlantOrderName on -> orderQuery.getByName(on).map(order -> observe(
                    base(on, lineage(on)).withOrder(PlantOrderView.of(order)).withChildren(familyChildren(on))));
        };
    }

    /** The rank-keyed attributes every Plant carries. Features are resolved over the
     *  pre-computed {@code ancestry} so the chain is walked only once. */
    private Plant base(PlantRankName name, Set<PlantRankName> ancestry) {
        return Plant.empty()
                .withFeatures(featureQuery.findByAncestry(name, ancestry))
                .withRole(roleQuery.forPlantName(name).orElse(null))
                .withImages(imageQuery.forParentName(name));
    }

    /** The subject's lineage as an ancestor-first ordered set (order → … → subject), built from
     *  the already-resolved ancestor names — mirrors {@link PlantAncestryResolver#ancestry}
     *  without a second walk. The ancestors are supplied subject-upward; each present one is the
     *  parent of the previous, so the chain stops at the first gap. */
    @SafeVarargs
    private Set<PlantRankName> lineage(PlantRankName subject, Optional<? extends PlantRankName>... ancestors) {
        List<PlantRankName> subjectFirst = new ArrayList<>();
        subjectFirst.add(subject);
        for (Optional<? extends PlantRankName> ancestor : ancestors) {
            ancestor.ifPresent(subjectFirst::add);
        }
        LinkedHashSet<PlantRankName> ancestorFirst = new LinkedHashSet<>();
        for (int i = subjectFirst.size() - 1; i >= 0; i--) {
            ancestorFirst.add(subjectFirst.get(i));
        }
        return ancestorFirst;
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

    private Plant observe(Plant plant) {
        observer.observable(plant, "plant").observe(Level.WARN);
        return plant;
    }
}
