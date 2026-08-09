package com.naturalist.soil.observation;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Multi-result return type for {@link SoilPhysicalCharacteristics} queries (ADR-011).
 */
public final class SoilPhysicalCharacteristicsCollection
        extends BehavioralCollection<SoilPhysicalCharacteristics> {

    SoilPhysicalCharacteristicsCollection(Collection<SoilPhysicalCharacteristics> characteristics) {
        super(characteristics);
    }

    public static SoilPhysicalCharacteristicsCollection of(
            Collection<SoilPhysicalCharacteristics> characteristics) {
        return new SoilPhysicalCharacteristicsCollection(characteristics);
    }

    public static SoilPhysicalCharacteristicsCollection empty() {
        return new SoilPhysicalCharacteristicsCollection(List.of());
    }
}
