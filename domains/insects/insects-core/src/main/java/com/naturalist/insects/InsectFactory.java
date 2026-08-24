package com.naturalist.insects;

import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
 *
 * <p>The ancestor rank chain is resolved <em>once</em> per build — each ancestor entity is
 * fetched a single time and its {@link InsectRankName} folded into the lineage set that
 * {@link InsectCitationQueryImpl#findByAncestry} and {@link InsectFeatureQueryImpl#findByAncestry}
 * both consume. Before this, the citation query, the feature query, and this factory each
 * walked the chain independently, so every ancestor's {@code getByName} fired three times per
 * page — the N+1 the runtime select gate flags.
 */
class InsectFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;
    private final InsectQuery.OrderQuery orderQuery;
    private final InsectLifeStageQuery lifeStageQuery;
    private final InsectCitationQueryImpl citationQuery;
    private final InsectFeatureQueryImpl featureQuery;
    private final InsectQuery.FunctionalRoleQuery roleQuery;

    InsectFactory(InsectQuery.SpeciesQuery speciesQuery,
                  InsectQuery.ImageQuery imageQuery,
                  InsectQuery.GenusQuery genusQuery,
                  InsectQuery.FamilyQuery familyQuery,
                  InsectQuery.OrderQuery orderQuery,
                  InsectLifeStageQuery lifeStageQuery,
                  InsectCitationQueryImpl citationQuery,
                  InsectFeatureQueryImpl featureQuery,
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
            case InsectSpeciesName sn -> speciesQuery.getByName(sn).map(species -> {
                Optional<InsectGenus> genus = genusQuery.getByName(species.genusName());
                Optional<InsectFamily> family = genus.flatMap(g -> familyQuery.getByName(g.familyName()));
                Optional<InsectOrder> order = family.flatMap(f -> orderQuery.getByName(f.orderName()));
                Set<InsectRankName> ancestry = lineage(sn,
                        genus.map(InsectGenus::name), family.map(InsectFamily::name), order.map(InsectOrder::name));
                Insect insect = base(sn, ancestry)
                        .withSpecies(InsectSpeciesView.of(species))
                        .withChildren(List.of());
                if (genus.isPresent()) insect = insect.withGenus(InsectGenusView.of(genus.get()));
                if (family.isPresent()) insect = insect.withFamily(InsectFamilyView.of(family.get()));
                if (order.isPresent()) insect = insect.withOrder(InsectOrderView.of(order.get()));
                return observe(insect);
            });
            case InsectGenusName gn -> genusQuery.getByName(gn).map(genus -> {
                Optional<InsectFamily> family = familyQuery.getByName(genus.familyName());
                Optional<InsectOrder> order = family.flatMap(f -> orderQuery.getByName(f.orderName()));
                Set<InsectRankName> ancestry = lineage(gn,
                        family.map(InsectFamily::name), order.map(InsectOrder::name));
                Insect insect = base(gn, ancestry)
                        .withGenus(InsectGenusView.of(genus))
                        .withChildren(speciesChildren(gn));
                if (family.isPresent()) insect = insect.withFamily(InsectFamilyView.of(family.get()));
                if (order.isPresent()) insect = insect.withOrder(InsectOrderView.of(order.get()));
                return observe(insect);
            });
            case InsectFamilyName fn -> familyQuery.getByName(fn).map(family -> {
                Optional<InsectOrder> order = orderQuery.getByName(family.orderName());
                Set<InsectRankName> ancestry = lineage(fn, order.map(InsectOrder::name));
                Insect insect = base(fn, ancestry)
                        .withFamily(InsectFamilyView.of(family))
                        .withChildren(genusChildren(fn));
                if (order.isPresent()) insect = insect.withOrder(InsectOrderView.of(order.get()));
                return observe(insect);
            });
            case InsectOrderName on -> orderQuery.getByName(on).map(order -> observe(
                    base(on, lineage(on)).withOrder(InsectOrderView.of(order)).withChildren(familyChildren(on))));
            case InsectSubspeciesName _ -> Optional.empty();
        };
    }

    /** The rank-keyed attributes every Insect carries, whatever the rank. Citations and features
     *  are resolved over the pre-computed {@code ancestry} so the chain is walked only once. */
    private Insect base(InsectRankName name, Set<InsectRankName> ancestry) {
        return Insect.empty()
                .withObservations(imageQuery.forParentName(name))
                .withLifeStages(lifeStageQuery.lifeStages().forParentName(name))
                .withCitations(citationQuery.findByAncestry(name, ancestry))
                .withFeatures(featureQuery.findByAncestry(name, ancestry))
                .withRole(roleQuery.getByParentName(name).orElse(null));
    }

    /** The subject's lineage as an ancestor-first ordered set (order → … → subject), built from
     *  the already-resolved ancestor names — mirrors {@link InsectAncestryResolver#ancestry}
     *  without a second walk. The ancestors are supplied subject-upward; each present one is the
     *  parent of the previous, so the chain stops at the first gap. */
    @SafeVarargs
    private Set<InsectRankName> lineage(InsectRankName subject, Optional<? extends InsectRankName>... ancestors) {
        List<InsectRankName> subjectFirst = new ArrayList<>();
        subjectFirst.add(subject);
        for (Optional<? extends InsectRankName> ancestor : ancestors) {
            ancestor.ifPresent(subjectFirst::add);
        }
        LinkedHashSet<InsectRankName> ancestorFirst = new LinkedHashSet<>();
        for (int i = subjectFirst.size() - 1; i >= 0; i--) {
            ancestorFirst.add(subjectFirst.get(i));
        }
        return ancestorFirst;
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

    private Insect observe(Insect insect) {
        observer.observable(insect, "insect").observe(Level.WARN);
        return insect;
    }
}
