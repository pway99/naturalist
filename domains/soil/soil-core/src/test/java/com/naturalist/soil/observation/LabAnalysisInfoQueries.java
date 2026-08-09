package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test-only wiring seam exposing a {@link LabAnalysisInfoQuery} from a {@link NaturalistDatabase}.
 * It lets full-graph wiring in the parent {@code com.naturalist.soil} package assemble the
 * observation sub-context without reaching its package-private impls directly — the same seam
 * pattern the insects {@code lifestage} sub-context uses.
 */
public final class LabAnalysisInfoQueries {

    private LabAnalysisInfoQueries() {
    }

    public static LabAnalysisInfoQuery create(NaturalistDatabase database) {
        return new LabAnalysisInfoQueryImpl(new LabAnalysisInfoEntityRepositoryMock(database));
    }
}
