package com.naturalist.soil.observation;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Multi-result return type for {@link LabAnalysisInfo} queries (ADR-011).
 */
public final class LabAnalysisInfoCollection extends BehavioralCollection<LabAnalysisInfo> {

    LabAnalysisInfoCollection(Collection<LabAnalysisInfo> analyses) {
        super(analyses);
    }

    public static LabAnalysisInfoCollection of(Collection<LabAnalysisInfo> analyses) {
        return new LabAnalysisInfoCollection(analyses);
    }

    public static LabAnalysisInfoCollection empty() {
        return new LabAnalysisInfoCollection(List.of());
    }
}
