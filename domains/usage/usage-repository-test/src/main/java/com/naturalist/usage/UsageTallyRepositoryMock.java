package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.NaturalistName;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

class UsageTallyRepositoryMock
        extends AbstractTestEntityRepository<UsageTallyId, UsageTally, UsageTallyTestEntitySource>
        implements UsageRepository.TallyRepository {

    UsageTallyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<UsageTally> findByCounterAndPeriods(
            UsageCounterName counter, Set<String> periods, @Nullable NaturalistName naturalist) {
        observer().arguments("findByCounterAndPeriods", i -> i
                        .identifier(counter, "counter")
                        .notNull(periods, "periods"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(t -> t.counter().equals(counter)
                        && periods.contains(t.period())
                        && (t.naturalist() == null || t.naturalist().equals(naturalist)))
                .toList();
    }
}
