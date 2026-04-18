package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The egg stage of an insect's life cycle.
 * <p>
 * {@code description} covers morphology and oviposition site — the minimum field
 * record. All remaining fields are nullable: not every taxon exhibits a documented
 * colour progression, a distinctive laying pattern, or an adaptive significance
 * worth recording at catalog level.
 * <p>
 * Notable example: Chrysoperla (green lacewing) eggs are laid on individual silk
 * stalks 10–15 mm tall — a structural adaptation that prevents newly hatched
 * predatory larvae from consuming unhatched siblings before they disperse.
 * The {@code adaptiveSignificance} field captures exactly this class of domain knowledge.
 */
public record EggStage(
        String description,
        @Nullable String colorProgression,
        @Nullable String layingPattern,
        @Nullable String adaptiveSignificance
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, EggStage::description, "description");
    }
}
