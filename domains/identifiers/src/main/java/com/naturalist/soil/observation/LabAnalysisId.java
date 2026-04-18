package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.PersistenceId;

public final class LabAnalysisId extends PersistenceId<Long> {

    private LabAnalysisId(Long value) {
        super(value);
    }

    @JsonCreator
    public static LabAnalysisId of(Long value) {
        return new LabAnalysisId(value);
    }
}
