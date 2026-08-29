package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectSpeciesRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the bottom-of-chain {@code insect_species} row, its upward {@code genus_id} FK, the
 * {@code getByGenusName} / {@code getByGenusNames} queries, and all five child tables
 * ({@code insect_species_common_name}, {@code insect_species_protected_stage},
 * {@code insect_species_habitat_zone}, {@code insect_species_habitat_layer},
 * {@code insect_species_supporting_plant}) plus the seven null-group value objects. Requires the
 * seeded standing DB; each test rolls back.
 */
class InsectSpeciesEntityRepositoryRdbmsIT implements InsectSpeciesRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.SpeciesRepository repository() {
        return new InsectSpeciesEntityRepositoryRdbms(rdbms.mapper(InsectSpeciesMapper.class));
    }
}
