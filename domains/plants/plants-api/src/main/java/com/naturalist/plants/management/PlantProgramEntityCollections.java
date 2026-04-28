package com.naturalist.plants.management;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the management sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface PlantProgramEntityCollections {

    final class PlantProgramCollection extends BehavioralCollection<PlantProgram> {

        PlantProgramCollection(Collection<PlantProgram> programs) {
            super(programs);
        }

        public static PlantProgramCollection of(Collection<PlantProgram> programs) {
            return new PlantProgramCollection(programs);
        }

        public static PlantProgramCollection empty() {
            return new PlantProgramCollection(List.of());
        }
    }
}
