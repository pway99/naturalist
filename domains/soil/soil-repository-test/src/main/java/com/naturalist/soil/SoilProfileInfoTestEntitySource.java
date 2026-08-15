package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * Test data source for {@link SoilProfileInfo} — the two documented Oak Vista soil profiles
 * (Box 1 and the back yard), seeded from real spatial associations. Both are zone-scoped: a
 * profile exists per physically sampled soil unit, and the backyard sub-zones are crop rows
 * sharing one bed. Backing catalog: {@code soil/profile/soil-profile-info.json}.
 */
public class SoilProfileInfoTestEntitySource extends TestEntitySource<SoilProfileName, SoilProfileInfo> {

    public SoilProfileInfoTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("soil/profile/soil-profile-info.json");
    }
}
