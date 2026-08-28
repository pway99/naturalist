package com.naturalist.naturalist;

import com.naturalist.RandomValue;
import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The behavioral contract ({@link NaturalistCredentialEntityRepositoryTest}) re-run against
 * real Postgres. Requires the standing DB to be seeded (apps/test-db-seeder); each test runs
 * in a transaction the extension rolls back. `source()` (inherited) is the in-memory JSON
 * oracle.
 *
 * <p>{@code newEntity()} is overridden: the shared contract's default builds a credential for
 * a random, non-existent {@link NaturalistName}, which is fine against the in-memory mock (no
 * FK) but writes zero rows against the real {@code naturalist} FK here. {@code sam-rivers} is
 * seeded with a naturalist row and deliberately no credential row, so it is a valid target for
 * a fresh insert.
 */
class NaturalistCredentialRepositoryRdbmsIT implements NaturalistCredentialEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public NaturalistRepository.CredentialRepository repository() {
        return new NaturalistCredentialRepositoryRdbms(rdbms.mapper(NaturalistCredentialMapper.class));
    }

    @Override
    public NaturalistCredential newEntity() {
        return new NaturalistCredential(NaturalistName.of("sam-rivers"), "{bcrypt}" + RandomValue.string());
    }
}
