package com.naturalist.insects.lifestage;

import com.naturalist.insects.LifeStageKind;

import java.util.List;

/**
 * Incomplete metamorphosis: an animal that hatches as a wingless nymph
 * resembling a small adult, growing through successive moults and
 * acquiring wings in the final transition to the adult stage. There is
 * no pupal rest. Includes the hemipteroids (true bugs, leafhoppers,
 * aphids), Odonata (dragonflies and damselflies), Orthoptera
 * (grasshoppers, crickets), and Blattodea (cockroaches and termites).
 */
public record Hemimetabolous() implements Metaboly {

    @Override
    public List<LifeStageKind> stages() {
        return List.of(
                LifeStageKind.EGG,
                LifeStageKind.NYMPH,
                LifeStageKind.ADULT);
    }
}
