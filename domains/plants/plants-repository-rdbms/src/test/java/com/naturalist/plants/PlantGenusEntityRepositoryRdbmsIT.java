package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantGenusRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the {@code plant_genus} row, its upward {@code family_id} FK, the locally-carried
 * {@code taxonomic_family} epithet, the {@code getByFamilyName} query, and the
 * {@code plant_genus_common_name} child. Requires the seeded standing DB; each test rolls back.
 */
class PlantGenusEntityRepositoryRdbmsIT implements PlantGenusRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.GenusRepository repository() {
        return new PlantGenusEntityRepositoryRdbms(rdbms.mapper(PlantGenusMapper.class));
    }
}
