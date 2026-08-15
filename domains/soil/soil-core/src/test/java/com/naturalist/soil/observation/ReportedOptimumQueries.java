package com.naturalist.soil.observation;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test-only wiring seam exposing a {@link ReportedOptimumQuery} from a {@link NaturalistDatabase},
 * so full-graph wiring in the parent {@code com.naturalist.soil} package can assemble the
 * observation sub-context without reaching its package-private impls directly.
 */
public final class ReportedOptimumQueries {

    private ReportedOptimumQueries() {
    }

    public static ReportedOptimumQuery create(NaturalistDatabase database) {
        return new ReportedOptimumQueryImpl(new ReportedOptimumEntityRepositoryMock(database));
    }
}
