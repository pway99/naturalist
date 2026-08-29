package com.naturalist.library;

import com.naturalist.insects.InsectsEntityRefResolver;
import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link CitationAssociationEntityRepositoryTest} behavioral contract re-run against real Postgres
 * — covers the surrogate-UUID key, the within-library citation FK (nested-select / JOIN), the flat
 * cross-domain subject columns reconstructed through the injected {@link InsectsEntityRefResolver},
 * and the getByCitationName / getBySubject / getBySubjects lookups. Requires the seeded standing DB;
 * each test rolls back.
 */
class CitationAssociationEntityRepositoryRdbmsIT implements CitationAssociationEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public CitationAssociationRepository repository() {
        return new CitationAssociationEntityRepositoryRdbms(
                rdbms.mapper(CitationAssociationMapper.class), new InsectsEntityRefResolver());
    }
}
