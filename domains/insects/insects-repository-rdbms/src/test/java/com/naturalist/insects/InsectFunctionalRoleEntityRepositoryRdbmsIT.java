package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectFunctionalRoleEntityRepositoryTest} behavioral contract re-run against real
 * Postgres — covers the surrogate-UUID key, the single-column {@code parent_name} unique constraint,
 * the {@code Set<FunctionalGuild>} child table, and the batched {@code getByParentNames} row-value
 * {@code IN}. Requires the seeded standing DB; each test rolls back.
 */
class InsectFunctionalRoleEntityRepositoryRdbmsIT implements InsectFunctionalRoleEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.FunctionalRoleRepository repository() {
        return new InsectFunctionalRoleEntityRepositoryRdbms(rdbms.mapper(InsectFunctionalRoleMapper.class));
    }
}
