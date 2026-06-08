package com.naturalist.insects;

import com.naturalist.ddd.ReadModel;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The Insect read model — sum-of-parts in-memory composition of everything known
 * about an insect at Oak Vista, at whatever identification depth has been
 * reached.
 *
 * <p>Composes:
 * <ul>
 *   <li>{@link ImageCollection observations} — the photographic working set
 *       driving identification (catalog-read use case loads the rank's images;
 *       workflow use case loads the naturalist's gathered photos).</li>
 *   <li>The rank chain — {@code @Nullable} {@link InsectOrderView},
 *       {@link InsectFamilyView}, {@link InsectGenusView},
 *       {@link InsectSpeciesView}. Populated to whatever depth
 *       identification has reached, monotonically filled from the top down.</li>
 *   <li>{@link LifeStageCollection lifeStages} — the
 *       {@link com.naturalist.insects.lifestage.LifeStage} records attached to
 *       this organism (potentially spanning ranks; each carries its own
 *       {@code parentName}).</li>
 * </ul>
 *
 * <p>Not persisted — the read model's constituent parts live in their own
 * repositories. {@code Insect} is the in-memory composition that orchestrates
 * invariants across them. {@code identifiedTo()} exposes the most-specific
 * identified rank's name; the {@code with*} mutators refine the read model
 * (workflow case) or replace constituent parts.
 *
 * <p><b>Construction is always permissible.</b> The record's canonical
 * constructor never throws — invalid states (null required collections,
 * monotonic-fill violations, etc.) are reported by {@link #invariants()} when
 * a consumer asks the {@link com.naturalist.observability.Observer} to walk
 * them. This is the project's standard pattern (see {@code domains/CLAUDE.md}
 * and {@code InsectSpeciesView} for reference). Consumers are responsible
 * for observing at boundaries before acting on a read model.
 *
 * <p>Structural invariants declared by {@link #invariants()}:
 * <ul>
 *   <li><b>Required collections</b> — {@code observations} and
 *       {@code lifeStages} must be non-null. Use
 *       {@link ImageCollection#empty()} /
 *       {@link LifeStageCollection#empty()} for the empty state.</li>
 *   <li><b>Per-rank descent</b> — each present rank is descended into once
 *       via its own {@code whenNotNull(rank, r -> r.readModel(rank, name))}
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
 *       {@code .genusBelongsToFamily}, {@code .speciesBelongsToGenus}.
 *       With the grandparent FKs removed there is exactly one path up the
 *       tree, so the former skip-level checks ({@code genusBelongsToOrder},
 *       {@code speciesBelongsToFamily}) are structurally impossible and
 *       gone.</li>
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
        @Nullable InsectOrderView order,
        @Nullable InsectFamilyView family,
        @Nullable InsectGenusView genus,
        @Nullable InsectSpeciesView species,
        LifeStageCollection lifeStages
) implements ReadModel {

    /**
     * Zero-state read model — empty observations, no rank identified, empty
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

    /** The order's name, if the read model carries an order. */
    public Optional<InsectOrderName> orderName() {
        return order == null ? Optional.empty() : Optional.of(order.name());
    }

    /** The family's name, if the read model carries a family. */
    public Optional<InsectFamilyName> familyName() {
        return family == null ? Optional.empty() : Optional.of(family.name());
    }

    /** The genus's name, if the read model carries a genus. */
    public Optional<InsectGenusName> genusName() {
        return genus == null ? Optional.empty() : Optional.of(genus.name());
    }

    /** The species's name, if the read model carries a species. */
    public Optional<InsectSpeciesName> speciesName() {
        return species == null ? Optional.empty() : Optional.of(species.name());
    }

    public Insect withObservations(ImageCollection observations) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withOrder(@Nullable InsectOrderView order) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withFamily(@Nullable InsectFamilyView family) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withGenus(@Nullable InsectGenusView genus) {
        return new Insect(observations, order, family, genus, species, lifeStages);
    }

    public Insect withSpecies(@Nullable InsectSpeciesView species) {
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
            // One descent per present rank (R1): a rank's own invariants are
            // walked exactly once, in its own block. Ancestor *presence* is a
            // notNull check (not a re-descent), and the single remaining FK
            // check per rank validates the immediate-parent typed FK. With the
            // grandparent FKs removed there is exactly one path up the tree, so
            // the former skip-level checks (speciesBelongsToFamily,
            // genusBelongsToOrder) are structurally impossible and gone.
            .whenNotNull(order, o -> o
                .readModel(order, "order")
            )
            .whenNotNull(family, f -> f
                .readModel(family, "family")
                .notNull(order, "family:order")
                .isTrue(family.belongsToOrder(order), "familyBelongsToOrder")
            )
            .whenNotNull(genus, g -> g
                .readModel(genus, "genus")
                .notNull(family, "genus:family")
                .isTrue(genus.belongsToFamily(family), "genusBelongsToFamily")
            )
            .whenNotNull(species, s -> s
                .readModel(species, "species")
                .notNull(genus, "species:genus")
                .isTrue(species.belongsToGenus(genus), "speciesBelongsToGenus")
            )
        ;
    }
}
