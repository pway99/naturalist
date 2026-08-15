package com.naturalist.soil.observation;

import com.naturalist.data.TestEntitySourceTest;

/**
 * One physical-characteristics row per analysis, and two analyses exist — so two rows. The two
 * that went away in the backyard collapse were byte-identical copies of the -002 row (CEC 34.2,
 * pH 7.2, EC 0.662), which is what made them worth deleting.
 */
class SoilPhysicalCharacteristicsTestEntitySourceTest
        extends TestEntitySourceTest<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics, SoilPhysicalCharacteristicsTestEntitySource> {

    @Override
    protected int minimumEntities() {
        return 2;
    }
}
