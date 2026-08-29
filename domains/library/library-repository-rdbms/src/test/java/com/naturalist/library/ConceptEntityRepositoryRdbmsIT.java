package com.naturalist.library;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link ConceptEntityRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the flat concept row and its four-column {@code Description}. Requires the seeded standing DB;
 * each test rolls back.
 */
class ConceptEntityRepositoryRdbmsIT implements ConceptEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public ConceptRepository repository() {
        return new ConceptEntityRepositoryRdbms(rdbms.mapper(ConceptMapper.class));
    }
}
