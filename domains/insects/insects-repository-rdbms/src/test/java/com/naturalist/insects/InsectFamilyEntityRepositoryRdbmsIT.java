package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectFamilyRepositoryTest} behavioral contract re-run against real Postgres — covers the
 * {@code insect_family} row, its upward {@code order_id} FK (nested-selected from the order name on
 * write, JOIN-projected on read), the nullable {@code placed_in} clade slug, the {@code getByOrderName}
 * / {@code getByOrderNames} queries, and the {@code insect_family_common_name} child. Requires the
 * seeded standing DB; each test rolls back.
 */
class InsectFamilyEntityRepositoryRdbmsIT implements InsectFamilyRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.FamilyRepository repository() {
        return new InsectFamilyEntityRepositoryRdbms(rdbms.mapper(InsectFamilyMapper.class));
    }
}
