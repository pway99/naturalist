package com.naturalist.soil.observation;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class LabAnalysisId extends EntityId {

    private LabAnalysisId(UUID value) {
        super(value);
    }

    public static LabAnalysisId of(UUID value) {
        return new LabAnalysisId(value);
    }

    public static LabAnalysisId create() {
        return new LabAnalysisId(EntityId.newUUID());
    }
}
