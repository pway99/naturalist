package com.naturalist.soil;

import com.naturalist.data.TestEntitySourceTest;

/**
 * Oak Vista has two managed soil units — Box 1 and the back yard — so the catalog holds two
 * profiles, not the default floor of four. The third and fourth rows used to be the backyard
 * sub-zone profiles, which were three views of one physical sample; padding the catalog back to
 * four would re-introduce exactly the duplication the collapse removed.
 */
class SoilProfileInfoTestEntitySourceTest
        extends TestEntitySourceTest<SoilProfileName, SoilProfileInfo, SoilProfileInfoTestEntitySource> {

    @Override
    protected int minimumEntities() {
        return 2;
    }
}
