package com.naturalist.soil.observation;

import com.naturalist.data.TestEntitySourceTest;

/**
 * Two real FGL analyses exist — CH 2671853-001 (Box 1) and -002 (back yard), both March 3, 2026.
 * Inventing a third to reach the default floor of four would put a lab report in the catalog that
 * no lab ever produced. The floor rises on its own when the August 2026 samples land.
 */
class LabAnalysisInfoTestEntitySourceTest
        extends TestEntitySourceTest<LabAnalysisId, LabAnalysisInfo, LabAnalysisInfoTestEntitySource> {

    @Override
    protected int minimumEntities() {
        return 2;
    }
}
