package com.naturalist.soil;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.List;

/**
 * Behavioral contract for {@link SoilProfileInfoRepository}. Inherits the
 * {@link EntityRepositoryTest} cases (ADR-002); supplies soil identity constants and entity
 * construction. No domain-specific query methods.
 */
interface SoilProfileInfoEntityRepositoryTest extends EntityRepositoryTest<SoilProfileName, SoilProfileInfo> {

    @Override
    SoilProfileInfoRepository repository();

    @Override
    default TestEntitySource<SoilProfileName, SoilProfileInfo> source() {
        return db.getNamed(SoilProfileInfoTestEntitySource.class);
    }

    @Override
    default SoilProfileName notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.soilProfile;
    }

    @Override
    default List<SoilProfileName> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.name,
                TestSoilIdentifiers.SoilProfiles.Backyard.name);
    }

    @Override
    default SoilProfileInfo newEntity() {
        return new SoilProfileInfo(
                SoilProfileName.of(RandomValue.string()),
                ZoneName.of(RandomValue.string()),
                null);
    }

    @Override
    default SoilProfileInfo ghostEntity() {
        return new SoilProfileInfo(
                SoilProfileName.of(RandomValue.string()),
                ZoneName.of(RandomValue.string()),
                null);
    }

    @Override
    default SoilProfileInfo modifiedEntity(SoilProfileInfo original) {
        return new SoilProfileInfo(
                original.name(),
                ZoneName.of(RandomValue.string()),
                SubZoneName.of(RandomValue.string()));
    }
}
