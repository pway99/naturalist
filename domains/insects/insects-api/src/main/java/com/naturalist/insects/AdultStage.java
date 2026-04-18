package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The adult stage of an insect's life cycle — the reproductive and (in many
 * species) the dispersal stage.
 * <p>
 * {@code feeding} describes the adult diet: nectarivore, predator, non-feeding
 * (many adult Ephemeroptera and some Lepidoptera are essentially non-feeding).
 * {@code role} is the adult's primary ecological function at Oak Vista —
 * pollinator, dispersal agent, reproductive stage only.
 * <p>
 * {@code attraction} captures stimuli that draw adults to specific microhabitats
 * or structures — artificial lighting (lacewings, crane flies), floral volatiles
 * (bees), or pheromone plumes. Relevant to siting habitat plantings and
 * managing light pollution effects on beneficial populations.
 * <p>
 * {@code supportedBy} lists the plant species or resource types that sustain
 * adult populations at Oak Vista. These will become typed cross-domain
 * {@code PlantName} references once the plants catalog is established.
 */
public record AdultStage(
        String feeding,
        String role,
        @Nullable String attraction,
        List<String> supportedBy
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(feeding, "feeding")
                .notBlank(role, "role")
                .notNull(supportedBy, "supportedBy");
    }
}
