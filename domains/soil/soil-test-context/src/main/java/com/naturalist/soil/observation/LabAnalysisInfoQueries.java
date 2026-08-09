package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Wiring seam exposing a {@link LabAnalysisInfoQuery} from a {@link NaturalistDatabase}, so
 * {@code SoilTestContext} (in the parent {@code com.naturalist.soil} package) can assemble the
 * observation sub-context without reaching its package-private impls/mocks directly.
 */
public final class LabAnalysisInfoQueries {

    private LabAnalysisInfoQueries() {
    }

    public static LabAnalysisInfoQuery create(NaturalistDatabase database) {
        return new LabAnalysisInfoQueryImpl(new LabAnalysisInfoEntityRepositoryMock(database));
    }
}
