package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Test data source for {@link Planting} — what actually went into the Oak Vista beds in 2026,
 * cross-checked against the cultivars the plants catalog carries and the zones soil samples.
 * <p>
 * The 2026 tomatoes are four plantings across two beds, three of them sharing the back yard, which
 * is the mixed-row case in fixture form. The summer herbs and the eggplant are still in the ground
 * and carry no {@code removedDate}. Backing catalog: {@code garden/planting.json}.
 */
public class PlantingTestEntitySource extends TestEntitySource<PlantingId, Planting> {

    public PlantingTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("garden/planting.json");
    }
}
