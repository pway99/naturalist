package com.naturalist.plants.management;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/** PlantProgram collection (N=1 collapse: top-level, no *EntityCollections namespace). */
public final class PlantProgramCollection extends BehavioralCollection<PlantProgram> {

    PlantProgramCollection(Collection<PlantProgram> items) {
        super(items);
    }

    public static PlantProgramCollection of(Collection<PlantProgram> items) {
        return new PlantProgramCollection(items);
    }

    public static PlantProgramCollection empty() {
        return new PlantProgramCollection(List.of());
    }
}
