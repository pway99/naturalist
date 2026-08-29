package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantFamilyRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the {@code plant_family} row, its upward {@code order_id} FK (nested-selected from the order name
 * on write, JOIN-projected on read), the {@code getByOrderName} query, and the
 * {@code plant_family_common_name} child. Requires the seeded standing DB; each test rolls back.
 */
class PlantFamilyEntityRepositoryRdbmsIT implements PlantFamilyRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.FamilyRepository repository() {
        return new PlantFamilyEntityRepositoryRdbms(rdbms.mapper(PlantFamilyMapper.class));
    }
}
