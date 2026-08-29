package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectGenusRepositoryTest} behavioral contract re-run against real Postgres — covers the
 * {@code insect_genus} row, its upward {@code family_id} FK, its own {@code taxonomic_genus} epithet
 * (no redundant family epithet), the nullable {@code placed_in} clade slug, the {@code getByFamilyName}
 * / {@code getByFamilyNames} queries, and the {@code insect_genus_common_name} child. Requires the
 * seeded standing DB; each test rolls back.
 */
class InsectGenusEntityRepositoryRdbmsIT implements InsectGenusRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.GenusRepository repository() {
        return new InsectGenusEntityRepositoryRdbms(rdbms.mapper(InsectGenusMapper.class));
    }
}
