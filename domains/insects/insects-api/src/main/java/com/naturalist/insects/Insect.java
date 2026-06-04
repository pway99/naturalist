package com.naturalist.insects;

import com.naturalist.ddd.Aggregate;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Insect aggregate — sum-of-parts in-memory composition of everything known
 * about an insect at Oak Vista, at whatever identification depth has been
 * reached.
 *
 * <p>Composes:
 * <ul>
 *   <li>{@link ImageCollection observations} — the photographic working set
 *       driving identification (catalog-read use case loads the rank's images;
 *       workflow use case loads the naturalist's gathered photos).</li>
 *   <li>The rank chain — {@code @Nullable} {@link InsectOrderAggregate},
 *       {@link InsectFamilyAggregate}, {@link InsectGenusAggregate},
 *       {@link InsectSpeciesAggregate}. Populated to whatever depth
 *       identification has reached, monotonically filled from the top down.</li>
 *   <li>{@link LifeStageCollection lifeStages} — the
 *       {@link com.naturalist.insects.lifestage.LifeStage} records attached to
 *       this organism (potentially spanning ranks; each carries its own
 *       {@code parentName}).</li>
 * </ul>
 *
 * <p>Not persisted — the aggregate's constituent parts live in their own
 * repositories. {@code Insect} is the in-memory composition that orchestrates
 * invariants across them. {@code identifiedTo()} exposes the most-specific
 * identified rank's name; the {@code with*} mutators refine the aggregate
 * (workflow case) or replace constituent parts.
 *
 * <p><b>Construction is always permissible.</b> The record's canonical
 * constructor never throws — invalid states (null required collections,
 * monotonic-fill violations, etc.) are reported by {@link #invariants()} when
 * a consumer asks the {@link com.naturalist.observability.Observer} to walk
 * them. This is the project's standard pattern (see {@code domains/CLAUDE.md}
 * and {@code InsectSpeciesAggregate} for reference). Consumers are responsible
 * for observing at boundaries before persisting / acting on an aggregate.
 *
 * <p>Structural invariants declared by {@link #invariants()}:
 * <ul>
 *   <li><b>Required collections</b> — {@code observations} and
 *       {@code lifeStages} must be non-null. Use
 *       {@link ImageCollection#empty()} /
 *       {@link LifeStageCollection#empty()} for the empty state.</li>
 *   <li><b>Per-rank descent</b> — each present rank is descended into once
 *       via its own {@code whenNotNull(rank, r -> r.aggregate(rank, name))}
 *       block. Closes the leaf-rank gap (the most-specific identified rank
 *       still gets its own invariants walked).</li>
 *   <li><b>Ancestor-presence</b> — when a child rank is set, each of its
 *       ancestor slots must also be set. Reported as path-prefixed
 *       {@code .child:ancestor} violations (e.g. {@code .species:genus},
 *       {@code .genus:family}) so the diagnostic identifies which child's
 *       expectation surfaced the missing ancestor.</li>
 *   <li><b>Cross-rank FK consistency</b> — when both child and ancestor are
 *       present, the child's typed ancestor-FK field must equal the
 *       ancestor's name. One {@code isTrue} check per direct FK on each
 *       rank entity: {@code .familyBelongsToOrder},
 *       {@code .genusBelongsToFamily}, {@code .genusBelongsToOrder},
 *       {@code .speciesBelongsToGenus}, {@code .speciesBelongsToFamily}.
 *       Each denormalized FK gets its own check — a {@code species.familyName}
 *       that drifts from the actual {@code family.name} is caught even when
 *       {@code species.genusName} and {@code genus.familyName} both still
 *       match.</li>
 * </ul>
 *
 * <p>Cross-rank clade invariants (placement-chain monotonicity, resolvable
 * metaboly, life-stage kind conformance to the resolved {@code Metaboly}) are
 * deferred to a follow-up effort — see
 * {@code docs/plans/insect-aggregate.md} phase 3.
 *
 * @see com.naturalist.insects.lifestage.InsectLifeStages for the clade-DAG
 *      walk-up resolver used by the deferred invariants
 */
public record Insect(
        ImageCollection observations,
        @Nullable InsectOrderAggregate order,
        @Nullable InsectFamilyAggregate family,
        @Nullable InsectGenusAggregate genus,
        @Nullable InsectSpeciesAggregate species,
        LifeStageCollection lifeStages
) implements Aggregate {

    /**
     * Zero-state aggregate — empty observations, no rank identified, empty
     * life stages. Useful as the starting point for progressive refinement
     * via the {@code with*} mutators.
     */
    public static Insect empty() {
        return new Insect(
                ImageCollection.empty(),
                null,
                null,
                null,
                null,
                LifeStageCollection.empty());
    }

    /**
     * Most-specific identified rank's typed name, if any. Empty when no rank
     * is set (observations-only workflow start).
     */
    public Optional<InsectRankName> identifiedTo() {
        if (species != null) return Optional.of(species.name());
        if (genus != null) return Optional.of(genus.name());
        if (family != null) return Optional.of(family.name());
        if (order != null) return Optional.of(order.name());
        return Optional.empty();
    }

    /** The order's name, if the aggregate carries an order. */
    public Optional<InsectOrderName> orderName() {
        return order == null ? Optional.empty() : Optional.of(order.name());
    }

    /** The family's name, if the aggregate carries a family. */
    public Optional<InsectFamilyName> familyName() {
        return family == null ? Optional.empty() : Optional.of(family.name());
    }

    /** The genus's name, if the aggregate carries a genus. */
    public Optional<InsectGenusName> genusName() {
        return genus == null ? Optional.empty() : Optional.of(genus.name());
    }

    /** The species's name, if the aggregate carries a species. */
    public Optional<InsectSpeciesName> speciesName() {
        return species == null ? Optional.empty() : Optional.of(species.name());
    }

    public Insect withObservations(ImageCollection observations) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withOrder(@Nullable InsectOrderAggregate order) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withFamily(@Nullable InsectFamilyAggregate family) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withGenus(@Nullable InsectGenusAggregate genus) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withSpecies(@Nullable InsectSpeciesAggregate species) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withLifeStages(LifeStageCollection lifeStages) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
            .behavioralCollection(observations, "observations")
            .behavioralCollection(lifeStages, "lifeStages")
            // One block per present rank. Each block uses aggregate(...) for
            // every rank reference — it does both the presence check (null
            // value fires a violation) and the descent into that rank's own
            // invariants. Path-prefixed names (e.g. "species:genus") tag the
            // descent with the child block that demanded it, so the same
            // underlying rank issue surfaces under multiple paths when
            // multiple descendants are present — useful diagnostic
            // traceability per consumer perspective.
            .whenNotNull(order, o -> o
                .aggregate(order, "order")
            )
            .whenNotNull(family, f -> f
                .aggregate(family, "family")
                .aggregate(order, "family:order")
                .isTrue(family.belongsToOrder(order), "familyBelongsToOrder")
            )
            .whenNotNull(genus, g -> g
                .aggregate(genus, "genus")
                .aggregate(family, "genus:family")
                .aggregate(order, "genus:order")
                .isTrue(genus.belongsToFamily(family), "genusBelongsToFamily")
                .isTrue(genus.belongsToOrder(order), "genusBelongsToOrder")
            )
            .whenNotNull(species, s -> s
                .aggregate(species, "species")
                .aggregate(genus, "species:genus")
                .aggregate(family, "species:family")
                .aggregate(order, "species:order")
                .isTrue(species.belongsToGenus(genus), "speciesBelongsToGenus")
                .isTrue(species.belongsToFamily(family), "speciesBelongsToFamily")
            )
        ;
    }
}
