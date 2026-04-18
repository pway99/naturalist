package com.naturalist.insects;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * The observable physical characteristics by which a species is identified in the field.
 * <p>
 * Features are ordered from most conspicuous to most diagnostic — the same sequence
 * a naturalist would follow when working through a field identification. Each entry
 * is a discrete, observable trait: colour, proportion, posture, structural feature.
 * <p>
 * These are morphological and postural facts, not behavioural ones. Behaviour belongs
 * in {@link LifeStages} or {@link InsectSpecies#sightingNotes()}.
 */
public record IdentificationFeatures(
        List<String> features
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.notNull(this, IdentificationFeatures::features, "features");
    }
}
