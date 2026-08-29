package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantObservationRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the surrogate-UUID key, the flat polymorphic subject, the nullable identification column group with
 * its candidate child table, and the getByNaturalist / getByNaturalistAndSubjects lookups. Requires the
 * seeded standing DB; each test rolls back.
 */
class PlantObservationEntityRepositoryRdbmsIT implements PlantObservationRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.ObservationRepository repository() {
        return new PlantObservationEntityRepositoryRdbms(rdbms.mapper(PlantObservationMapper.class));
    }
}
