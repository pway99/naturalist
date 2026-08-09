package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Test data source for {@link LabAnalysisInfo} — the real FGL March 2026 soil chemistry, one
 * analysis per Oak Vista profile (Box 1 sample {@code CH 2671853-001}; the three backyard
 * sub-zones share the physical {@code CH 2671853-002} sample). Backing catalog:
 * {@code soil/observation/lab-analysis-info.json}.
 */
public class LabAnalysisInfoTestEntitySource extends TestEntitySource<LabAnalysisId, LabAnalysisInfo> {

    public LabAnalysisInfoTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("soil/observation/lab-analysis-info.json");
    }
}
