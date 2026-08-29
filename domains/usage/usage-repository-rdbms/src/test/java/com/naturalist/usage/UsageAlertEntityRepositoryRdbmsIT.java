package com.naturalist.usage;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link UsageAlertRepositoryTest} behavioral contract re-run against real Postgres. Like events,
 * alerts carry no persistent JSON seed — the contract inserts two fixtures into the in-memory
 * {@code source()} in a {@code @BeforeEach}, which this IT mirrors into the rolled-back DB session.
 */
class UsageAlertEntityRepositoryRdbmsIT implements UsageAlertRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public UsageRepository.AlertRepository repository() {
        return new UsageAlertEntityRepositoryRdbms(rdbms.mapper(UsageAlertMapper.class));
    }

    @BeforeEach
    @Override
    public void seedKnownAlerts() {
        UsageAlertRepositoryTest.super.seedKnownAlerts();         // populate the in-memory source()
        source().entityStream().forEach(repository()::insert);    // mirror into the rolled-back DB session
    }
}
