package com.naturalist.library;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link CitationEntityRepositoryTest} behavioral contract re-run against real Postgres — covers
 * the sealed {@code kind} discriminator round-trip, the flattened {@code AuthorityReference}, and the
 * nullable {@code Instant} {@code lastModified} column. Requires the seeded standing DB; each test
 * rolls back.
 */
class CitationEntityRepositoryRdbmsIT implements CitationEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public CitationRepository repository() {
        return new CitationEntityRepositoryRdbms(rdbms.mapper(CitationMapper.class));
    }
}
