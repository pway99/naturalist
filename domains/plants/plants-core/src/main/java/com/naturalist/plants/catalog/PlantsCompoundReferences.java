package com.naturalist.plants.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityReferences;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.EntityName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantRankName;
import com.naturalist.plants.PlantsDomain;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentEntityCollections.PhytochemicalConstituentCollection;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;
import com.naturalist.resilience.Resilient;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Inverse-direction catalog provider for the plants domain — the M5 milestone's
 * first real {@link EntityReferences} implementation. Answers "which plants
 * domain entities reference this {@link CompoundName}?" by querying the
 * phytochemistry sub-context's constituent index.
 *
 * <h2>Two refs per match</h2>
 * Per the plan's M5 recommendation, each matching constituent contributes
 * <em>two</em> {@link EntityRef}s to the result:
 * <ul>
 *   <li>a {@link PlantRankName}-typed ref to the plant that produces the compound
 *       — the navigation target, what the chemistry detail page links to first;</li>
 *   <li>a {@link PhytochemicalConstituentName}-typed ref to the constituent
 *       record itself — the detail target, where the role / tissue / induction
 *       story lives.</li>
 * </ul>
 * The two refs share {@link PlantsDomain} but are distinguishable by the
 * runtime class of their underlying name. Consumers that only want
 * navigational anchors filter by {@code instanceof PlantRankName} on
 * {@link EntityRef#name()}; consumers rendering the full constituent
 * description filter by {@code instanceof PhytochemicalConstituentName}.
 *
 * <h2>Plant deduplication</h2>
 * A single plant may produce multiple constituents of the same compound (a
 * compound expressed in two induction modes is recorded as two
 * {@link PhytochemicalConstituent}s sharing a {@code plantName}). The plant
 * appears <em>once</em> in the result regardless. Constituents are not
 * deduplicated — each is its own record and the consumer is entitled to render
 * each one. Iteration order is the order returned by the constituent query;
 * the deduplication key is the {@link PlantRankName} value, applied via a
 * {@link LinkedHashSet} so the first-seen plant ref is the one emitted.
 *
 * <h2>Live, not cached</h2>
 * The kernel's contract is that the catalog re-queries on every fan-out — see
 * {@link EntityReferences} javadoc. No caching here either; a freshly inserted
 * constituent is visible to the next chemistry detail page render.
 *
 * <h2>Empty target</h2>
 * A {@code null} {@code target} short-circuits to an empty stream. The kernel
 * already maps {@code null} to an empty result at the {@code Catalog} surface,
 * so this is purely defensive — calling the underlying query with {@code null}
 * would trip its argument observer and throw.
 */
@DomainService
@Resilient(name = "catalog.fanout")
public class PlantsCompoundReferences implements EntityReferences<CompoundName> {

    private static final DomainId DOMAIN = new PlantsDomain();

    private final PhytochemicalConstituentQuery.PhytochemicalConstituentEntityQuery constituents;

    public PlantsCompoundReferences(PhytochemicalConstituentQuery.PhytochemicalConstituentEntityQuery constituents) {
        Observer.forClass(PlantsCompoundReferences.class)
                .arguments("constructor", i -> i.notNull(constituents, "constituents"))
                .throwWhenInvalid();
        this.constituents = constituents;
    }

    @Override
    public DomainId domain() {
        return DOMAIN;
    }

    @Override
    public Class<CompoundName> referenceType() {
        return CompoundName.class;
    }

    @Override
    public Stream<EntityRef> referencesTo(CompoundName target) {
        if (target == null) {
            return Stream.empty();
        }
        PhytochemicalConstituentCollection matches = constituents.forCompoundName(target);

        Set<PlantRankName> seenPlants = new LinkedHashSet<>();
        Stream.Builder<EntityRef> refs = Stream.builder();
        matches.stream().forEach(constituent -> {
            if (seenPlants.add(constituent.plantName())) {
                // EntityRef takes an EntityName; PlantRankName is a sibling interface, so
                // the cast is unavoidable. Every permit extends EntityName, making it safe.
                // insects casts the same way — see InsectIdentificationCommand.writeCitations.
                refs.add(new EntityRef(DOMAIN, (EntityName) constituent.plantName()));
            }
            refs.add(new EntityRef(DOMAIN, constituent.name()));
        });
        return refs.build();
    }
}
