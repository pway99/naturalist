package com.naturalist.usage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.NaturalistName;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

class UsageTallyRepositoryMock
        extends AbstractTestEntityRepository<UsageTallyId, UsageTally, UsageTallyTestEntitySource>
        implements UsageRepository.TallyRepository {

    UsageTallyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public Optional<UsageTally> findBusinessKey(
            UsageCounterName counter, @Nullable NaturalistName naturalist, String period) {
        observer().arguments("findBusinessKey", i -> i.identifier(counter, "counter").notBlank(period, "period"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(t -> t.counter().equals(counter)
                        && Objects.equals(t.naturalist(), naturalist)
                        && t.period().equals(period))
                .findFirst();
    }
}
