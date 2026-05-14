package com.naturalist.insects.lifestage;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.insects.InsectClades;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.LifeStageKind;

import java.util.List;
import java.util.Optional;

/**
 * Resolver answering "which life-stage kinds does this organism have?" by
 * walking the organism's {@code placedIn} clade up through
 * {@link CladeTraversal} until a {@link MetabolyTrait} declaration is found,
 * then returning the {@link Metaboly#stages()} list from that declaration.
 *
 * <p>This is the consumer-facing surface introduced in Phase 5 of the
 * clades-kernel refactor. It replaces the implicit answer that the inline
 * {@code egg}/{@code larva}/{@code pupa}/{@code adult} fields on
 * {@link InsectSpecies}, {@link InsectGenus}, and {@link InsectFamily}
 * previously gave — those fields remain populated for the time being but
 * are no longer the source of truth for "what stages exist". Per-organism
 * stage <i>data</i> (descriptions, phenology, habitat, chemistry roles)
 * continues to live on the {@code LifeStage} records keyed by
 * {@code (organismName, stageKind)}; only the *kind enumeration* moves
 * to clade resolution.
 *
 * <p>An unplaced organism — or one placed in a clade with no
 * {@code MetabolyTrait} declaration anywhere up the chain — resolves to
 * an empty list. There is no exception path: "no resolvable stages" is
 * a legitimate state for in-progress catalog entries.
 *
 * <p>Mirrors the {@link InsectClades} static-utility shape: pure
 * function from input record to output list, no state, no DI.
 *
 * @see InsectClades#traitsFor(Clade)
 * @see Metaboly
 * @see MetabolyTrait
 */
public final class InsectLifeStages {

    private InsectLifeStages() {
    }

    public static List<LifeStageKind> stagesOf(InsectSpecies species) {
        return resolve(species.placedInOptional());
    }

    public static List<LifeStageKind> stagesOf(InsectGenus genus) {
        return resolve(genus.placedInOptional());
    }

    public static List<LifeStageKind> stagesOf(InsectFamily family) {
        return resolve(family.placedInOptional());
    }

    private static List<LifeStageKind> resolve(Optional<Clade> placedIn) {
        return placedIn
                .flatMap(clade -> CladeTraversal.findTrait(
                        clade, MetabolyTrait.class, InsectClades::traitsFor))
                .map(trait -> trait.metaboly().stages())
                .orElse(List.of());
    }
}
