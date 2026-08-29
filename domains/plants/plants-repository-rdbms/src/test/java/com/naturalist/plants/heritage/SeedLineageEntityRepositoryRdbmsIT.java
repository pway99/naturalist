package com.naturalist.plants.heritage;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantSeedLineageRepositoryTest} behavioral contract re-run against real Postgres —
 * exercises the {@code cultivar} FK join, the flattened {@code provenance_*} columns, and the
 * reverse getByCultivarName lookup. Requires the seeded standing DB; each test rolls back.
 */
class SeedLineageEntityRepositoryRdbmsIT implements PlantSeedLineageRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public SeedLineageRepository repository() {
        return new SeedLineageEntityRepositoryRdbms(rdbms.mapper(SeedLineageMapper.class));
    }
}
