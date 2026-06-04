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
 *   <li><b>Monotonic fill</b> — if {@code species} is set, {@code genus} must
 *       be set; if {@code genus} is set, {@code family} must be set; if
 *       {@code family} is set, {@code order} must be set. Each rule reports
 *       its violation as the missing field's name (e.g. {@code .genus} when
 *       species is present but genus is null) — the consumer infers the
 *       cross-rank semantic from context.</li>
 *   <li><b>Constituent descent</b> — descends into each non-null constituent
 *       (observations, present rank aggregates, lifeStages) so the Observer
 *       walks their own invariants as part of validating the aggregate.</li>
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
        return i -> {
            // Required collections: null → violation; non-null → descend.
            i.observable(observations, "observations");
            i.observable(lifeStages, "lifeStages");
            // Monotonic fill: each level conditional on the level below being present.
            // Reports the immediate missing field; consumer infers the cross-rank semantic.
            if (species != null) i.notNull(genus, "genus");
            if (genus != null) i.notNull(family, "family");
            if (family != null) i.notNull(order, "order");
            // Descent into present rank aggregates.
            if (order != null) i.observable(order, "order");
            if (family != null) i.observable(family, "family");
            if (genus != null) i.observable(genus, "genus");
            if (species != null) i.observable(species, "species");
        };
    }
}
