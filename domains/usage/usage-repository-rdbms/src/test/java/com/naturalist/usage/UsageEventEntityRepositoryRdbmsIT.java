package com.naturalist.usage;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The {@link UsageEventRepositoryTest} behavioral contract re-run against real Postgres. Events carry no
 * persistent JSON seed — the contract inserts two fixtures into the in-memory {@code source()} in a
 * {@code @BeforeEach}. This IT mirrors that source into the rolled-back DB session so the DB-backed
 * repository sees the same fixtures each test.
 */
class UsageEventEntityRepositoryRdbmsIT implements UsageEventRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public UsageRepository.EventRepository repository() {
        return new UsageEventEntityRepositoryRdbms(rdbms.mapper(UsageEventMapper.class));
    }

    @BeforeEach
    @Override
    public void seedKnownEvents() {
        UsageEventRepositoryTest.super.seedKnownEvents();          // populate the in-memory source()
        source().entityStream().forEach(repository()::insert);    // mirror into the rolled-back DB session
    }
}
