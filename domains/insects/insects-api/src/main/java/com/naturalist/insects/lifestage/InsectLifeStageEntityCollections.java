package com.naturalist.insects.lifestage;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the insect life stage sub-context's {@link BehavioralCollection}
 * return types. Mirrors {@code InsectEntityCollections} — one file per namespace,
 * nested types for everything inside.
 */
public interface InsectLifeStageEntityCollections {

    final class LifeStageCollection extends BehavioralCollection<LifeStage> {

        LifeStageCollection(Collection<LifeStage> stages) {
            super(stages);
        }

        public static LifeStageCollection of(Collection<LifeStage> stages) {
            return new LifeStageCollection(stages);
        }

        public static LifeStageCollection empty() {
            return new LifeStageCollection(List.of());
        }
    }
}