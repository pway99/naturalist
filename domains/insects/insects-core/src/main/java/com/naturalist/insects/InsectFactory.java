package com.naturalist.insects;

import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Name-keyed, rank-polymorphic assembly of the full {@link Insect} read model.
 * Resolves the rank chain from the given {@link InsectRankName} upward to the
 * order, fetches images, life stages, and citations, then composes the result
 * into an {@link Insect}.
 *
 * <ul>
 *   <li>{@link InsectSpeciesName}  → species + genus + family + order</li>
 *   <li>{@link InsectGenusName}    → genus + family + order</li>
 *   <li>{@link InsectFamilyName}   → family + order</li>
 *   <li>{@link InsectOrderName}    → order only</li>
 *   <li>{@link InsectSubspeciesName} → {@link Optional#empty()} (no entity exists yet)</li>
 * </ul>
 */
class InsectFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;
    private final InsectQuery.OrderQuery orderQuery;
    private final InsectLifeStageQuery lifeStageQuery;
    private final InsectQuery.CitationQuery citationQuery;
    private final InsectQuery.FeatureQuery featureQuery;
    private final InsectQuery.FunctionalRoleQuery roleQuery;

    InsectFactory(InsectQuery.SpeciesQuery speciesQuery,
                  InsectQuery.ImageQuery imageQuery,
                  InsectQuery.GenusQuery genusQuery,
                  InsectQuery.FamilyQuery familyQuery,
                  InsectQuery.OrderQuery orderQuery,
                  InsectLifeStageQuery lifeStageQuery,
                  InsectQuery.CitationQuery citationQuery,
                  InsectQuery.FeatureQuery featureQuery,
                  InsectQuery.FunctionalRoleQuery roleQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(lifeStageQuery, "lifeStageQuery")
                        .notNull(citationQuery, "citationQuery")
                        .notNull(featureQuery, "featureQuery")
                        .notNull(roleQuery, "roleQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
        this.orderQuery = orderQuery;
        this.lifeStageQuery = lifeStageQuery;
        this.citationQuery = citationQuery;
        this.featureQuery = featureQuery;
        this.roleQuery = roleQuery;
    }

    Optional<Insect> buildByName(InsectRankName name) {
        observer.arguments("buildByName", i -> i.identifier(name, "name")).throwWhenInvalid();
        return switch (name) {
            case InsectSpeciesName sn -> speciesQuery.getByName(sn).map(s -> observe(
                    resolveGenus(base(sn).withSpecies(InsectSpeciesView.of(s)).withChildren(List.of()),
                            s.genusName())));
            case InsectGenusName gn -> genusQuery.getByName(gn).map(g -> observe(
                    resolveFamily(base(gn).withGenus(InsectGenusView.of(g)).withChildren(speciesChildren(gn)),
                            g.familyName())));
            case InsectFamilyName fn -> familyQuery.getByName(fn).map(f -> observe(
                    resolveOrder(base(fn).withFamily(InsectFamilyView.of(f)).withChildren(genusChildren(fn)),
                            f.orderName())));
            case InsectOrderName on -> orderQuery.getByName(on).map(o -> observe(
                    base(on).withOrder(InsectOrderView.of(o)).withChildren(familyChildren(on))));
            case InsectSubspeciesName _ -> Optional.empty();
        };
    }

    /** The rank-keyed attributes every Insect carries, whatever the rank. */
    private Insect base(InsectRankName name) {
        return Insect.empty()
                .withObservations(imageQuery.forParentName(name))
                .withLifeStages(lifeStageQuery.lifeStages().forParentName(name))
                .withCitations(citationQuery.findByRankName(name))
                .withFeatures(featureQuery.findByRankName(name))
                .withRole(roleQuery.getByParentName(name).orElse(null));
    }

    private List<InsectTaxonView> familyChildren(InsectOrderName orderName) {
        return familyQuery.forOrderName(orderName).stream()
                .map(f -> (InsectTaxonView) InsectFamilyView.of(
                        f, imageQuery.forRankHierarchy(f.name())))
                .sorted(Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private List<InsectTaxonView> genusChildren(InsectFamilyName familyName) {
        return genusQuery.forFamilyName(familyName).stream()
                .map(g -> (InsectTaxonView) InsectGenusView.of(
                        g, imageQuery.forRankHierarchy(g.name())))
                .sorted(Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private List<InsectTaxonView> speciesChildren(InsectGenusName genusName) {
        return speciesQuery.forGenusName(genusName).stream()
                .map(s -> (InsectTaxonView) InsectSpeciesView.of(
                        s, imageQuery.forParentName(s.name())))
                .sorted(Comparator.comparing(v -> v.name().value()))
                .toList();
    }

    private Insect resolveGenus(Insect insect, InsectGenusName genusName) {
        return genusQuery.getByName(genusName)
                .map(genus -> resolveFamily(
                        insect.withGenus(InsectGenusView.of(genus)),
                        genus.familyName()))
                .orElse(insect);
    }

    private Insect resolveFamily(Insect insect, InsectFamilyName familyName) {
        return familyQuery.getByName(familyName)
                .map(family -> resolveOrder(
                        insect.withFamily(InsectFamilyView.of(family)),
                        family.orderName()))
                .orElse(insect);
    }

    private Insect resolveOrder(Insect insect, InsectOrderName orderName) {
        return orderQuery.getByName(orderName)
                .map(order -> insect.withOrder(InsectOrderView.of(order)))
                .orElse(insect);
    }

    private Insect observe(Insect insect) {
        observer.observable(insect, "insect").observe(Level.WARN);
        return insect;
    }
}
