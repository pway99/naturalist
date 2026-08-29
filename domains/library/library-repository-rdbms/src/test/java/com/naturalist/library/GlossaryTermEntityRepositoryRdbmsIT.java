package com.naturalist.library;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link GlossaryTermEntityRepositoryTest} behavioral contract re-run against real Postgres —
 * covers the flat glossary-term row and its nullable {@code example}. Requires the seeded standing
 * DB; each test rolls back.
 */
class GlossaryTermEntityRepositoryRdbmsIT implements GlossaryTermEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public GlossaryTermRepository repository() {
        return new GlossaryTermEntityRepositoryRdbms(rdbms.mapper(GlossaryTermMapper.class));
    }
}
