package com.naturalist.chemistry.product;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link ProductEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * exercises the formulation join table, the properties map, and the reverse getByCompoundName
 * lookup. Requires the seeded standing DB; each test rolls back.
 */
class ProductEntityRepositoryRdbmsIT implements ProductEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public ProductRepository repository() {
        return new ProductEntityRepositoryRdbms(rdbms.mapper(ProductMapper.class));
    }
}
