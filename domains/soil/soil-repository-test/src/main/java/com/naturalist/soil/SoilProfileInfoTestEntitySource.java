package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Test data source for {@link SoilProfileInfo} — the four documented Oak Vista soil profiles
 * (Box 1 and the three backyard sub-zones), seeded from real spatial associations. Backing
 * catalog: {@code soil/profile/soil-profile-info.json}.
 */
public class SoilProfileInfoTestEntitySource extends TestEntitySource<SoilProfileName, SoilProfileInfo> {

    public SoilProfileInfoTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("soil/profile/soil-profile-info.json");
    }
}
