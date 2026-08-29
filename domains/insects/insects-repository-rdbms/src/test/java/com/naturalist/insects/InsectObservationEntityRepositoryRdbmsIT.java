package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectObservationEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the surrogate-UUID key, the flat polymorphic subject, the Identification column group with its
 * candidate child table (insects seed data carries non-null identifications), and the getByNaturalist /
 * getByNaturalistAndSubjects lookups. Requires the seeded standing DB; each test rolls back.
 */
class InsectObservationEntityRepositoryRdbmsIT implements InsectObservationEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.ObservationRepository repository() {
        return new InsectObservationEntityRepositoryRdbms(rdbms.mapper(InsectObservationMapper.class));
    }
}
