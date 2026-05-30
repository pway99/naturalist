package com.naturalist.insects.lifestage;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.insects.InsectClades;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectOrder;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.LifeStageKind;
import org.jspecify.annotations.Nullable;

import java.util.List;

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
        return resolve(species.placedIn());
    }

    public static List<LifeStageKind> stagesOf(InsectGenus genus) {
        return resolve(genus.placedIn());
    }

    public static List<LifeStageKind> stagesOf(InsectFamily family) {
        return resolve(family.placedIn());
    }

    /**
     * Resolves life-stage kinds for a species by inheriting {@code placedIn}
     * up the Linnaean parent chain. Picks the first non-null placement
     * walking species → genus → family → order, then runs the clade-DAG
     * traversal from there.
     *
     * <p>The {@code species} parameter is non-null — it is the subject of the
     * query. The three parent ranks are nullable to accommodate partial
     * inputs (controller short-circuits, test fixtures where the chain is not
     * fully assembled). When the entire chain has no placement, the resolver
     * returns an empty list — the same "no exception path" contract the
     * single-rank overloads honour.
     *
     * <p>Wired into {@code InsectsController.detail(...)} in PR 2 of the
     * Phase 5b slice. Replaces silent empties on species-detail pages whose
     * placement is declared at a higher rank than the species itself.
     *
     * @see #stagesOf(InsectSpecies) for the narrow "this entity's own placement" semantics
     */
    public static List<LifeStageKind> stagesOf(
            InsectSpecies species,
            @Nullable InsectGenus genus,
            @Nullable InsectFamily family,
            @Nullable InsectOrder order) {

        Clade placement = firstNonNull(
                species.placedIn(),
                genus  != null ? genus.placedIn()  : null,
                family != null ? family.placedIn() : null,
                order  != null ? order.placedIn()  : null);
        return resolve(placement);
    }

    private static @Nullable Clade firstNonNull(@Nullable Clade... candidates) {
        for (Clade candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static List<LifeStageKind> resolve(@Nullable Clade placedIn) {
        if (placedIn == null) {
            return List.of();
        }
        return CladeTraversal.findTrait(placedIn, MetabolyTrait.class, InsectClades::traitsFor)
                .map(trait -> trait.metaboly().stages())
                .orElse(List.of());
    }
}
