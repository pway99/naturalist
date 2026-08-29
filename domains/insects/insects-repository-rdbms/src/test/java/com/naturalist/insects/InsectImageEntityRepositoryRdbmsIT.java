package com.naturalist.insects;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link InsectImageRepositoryTest} behavioral contract re-run against real Postgres — covers the
 * surrogate-UUID key, the flat polymorphic parent reference, the optional observation FK, and the
 * getByParentName / getByParentNames lookups. Requires the seeded standing DB; each test rolls back.
 */
class InsectImageEntityRepositoryRdbmsIT implements InsectImageRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public InsectRepository.ImageRepository repository() {
        return new InsectImageEntityRepositoryRdbms(rdbms.mapper(InsectImageMapper.class));
    }
}
