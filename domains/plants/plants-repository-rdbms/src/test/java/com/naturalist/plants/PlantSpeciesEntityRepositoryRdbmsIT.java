package com.naturalist.plants;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link PlantSpeciesRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the bottom-of-chain {@code plant_species} row, its upward {@code genus_id} FK, the
 * {@code getByGenusName} / {@code getByGenusNames} queries, and both child tables
 * ({@code plant_species_common_name}, {@code plant_species_native_bioregion}). Requires the seeded
 * standing DB; each test rolls back.
 */
class PlantSpeciesEntityRepositoryRdbmsIT implements PlantSpeciesRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public PlantRepository.SpeciesRepository repository() {
        return new PlantSpeciesEntityRepositoryRdbms(rdbms.mapper(PlantSpeciesMapper.class));
    }
}
