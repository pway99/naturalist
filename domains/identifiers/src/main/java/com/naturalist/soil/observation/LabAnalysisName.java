package com.naturalist.soil.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class LabAnalysisName extends FactName {

    private LabAnalysisName(UUID value) {
        super(value);
    }

    @JsonCreator
    public static LabAnalysisName of(UUID value) {
        return new LabAnalysisName(value);
    }
}
