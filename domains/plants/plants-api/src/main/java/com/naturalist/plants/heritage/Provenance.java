package com.naturalist.plants.heritage;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The origin and generational history of a seed lineage.
 * <p>
 * Provenance records who maintained the lineage, where, and for how many
 * generations. This is the chain of custody for irreplaceable genetic
 * heritage — Nick's Italian Pear traces back 50+ years of continuous
 * selection in coastal California.
 * <p>
 * {@code estimatedGenerations} is approximate for long-running family
 * lineages where exact annual records were not kept. For new adaptation
 * programs (Chico 2026 onward), generation count will be exact.
 */
public record Provenance(
        String originator,
        String originLocation,
        int estimatedGenerations,
        @Nullable String sourceNotes
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(originator, "originator")
                .notBlank(originLocation, "originLocation");
    }
}
