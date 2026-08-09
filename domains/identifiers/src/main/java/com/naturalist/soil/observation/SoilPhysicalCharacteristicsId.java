package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/**
 * Surrogate identity of a {@code SoilPhysicalCharacteristics} row. There is one per
 * {@code LabAnalysis}; the {@code labAnalysisId} back-reference is enforced unique.
 */
public final class SoilPhysicalCharacteristicsId extends EntityId {

    private SoilPhysicalCharacteristicsId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static SoilPhysicalCharacteristicsId of(UUID value) {
        return new SoilPhysicalCharacteristicsId(value);
    }

    public static SoilPhysicalCharacteristicsId create() {
        return new SoilPhysicalCharacteristicsId(EntityId.newUUID());
    }
}
