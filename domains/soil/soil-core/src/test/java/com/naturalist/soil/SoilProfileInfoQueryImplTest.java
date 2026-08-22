package com.naturalist.soil;

import com.naturalist.data.EntityQuery;
import com.naturalist.data.EntityQueryContractTest;
import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

class SoilProfileInfoQueryImplTest
        implements EntityQueryContractTest<SoilProfileName, SoilProfileInfo, SoilProfileInfoCollection> {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    SoilProfileInfoQuery query =
            new SoilProfileInfoQueryImpl(new SoilProfileInfoEntityRepositoryMock(db));

    @Override
    public EntityQuery<SoilProfileName, SoilProfileInfo, SoilProfileInfoCollection> query() {
        return query;
    }

    @Override
    public SoilProfileName notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.soilProfile;
    }

    @Override
    public List<SoilProfileName> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.name,
                TestSoilIdentifiers.SoilProfiles.Backyard.name);
    }
}
