package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.NaturalistName;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

@MockDomainService
class UsageEventRepositoryMock
        extends AbstractTestEntityRepository<UsageEventId, UsageEvent, UsageEventTestEntitySource>
        implements UsageRepository.EventRepository {

    UsageEventRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<UsageEvent> findByCounterSince(UsageCounterName counter,
                                               @Nullable NaturalistName naturalist,
                                               Instant since) {
        observer().arguments("findByCounterSince", i -> i
                        .identifier(counter, "counter")
                        .notNull(since, "since"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(e -> e.counterName().equals(counter)
                        && !e.instant().isBefore(since)
                        && (naturalist == null || e.naturalist().equals(naturalist)))
                .toList();
    }
}
