package com.naturalist.insects.lifestage;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectLifeStageEntityRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the sealed single-table hierarchy (egg/larva/pupa/adult discriminated by {@code stage_kind}), the phenology
 * window / habitat zone-layer / host-plant / parasitoid-host / nectar-source child tables, the flattened
 * DiapauseRegulation, and the getByParentName lookup. Requires the seeded standing DB; each test rolls back.
 */
class InsectLifeStageEntityRepositoryRdbmsIT implements InsectLifeStageEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public LifeStageRepository.LifeStageEntityRepository repository() {
        return new InsectLifeStageEntityRepositoryRdbms(rdbms.mapper(InsectLifeStageMapper.class));
    }
}
