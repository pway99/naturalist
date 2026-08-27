package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class UsageCounterTestEntitySource extends TestEntitySource<UsageCounterId, UsageCounter> {

    public UsageCounterTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("usage/usage-counters.json");
    }

    /** At most one rule per (counterName, scope, windowKind) combination. */
    @Override
    protected List<UniqueConstraint<UsageCounter>> uniqueConstraints() {
        return List.of(new UniqueConstraint<>() {
            @Override
            public String name() {
                return "counterName+scope+windowKind";
            }

            @Override
            public Function<UsageCounter, ?> valueFunction() {
                return c -> c.counterName().value() + ":" + c.scope() + ":" + c.windowKind();
            }
        });
    }
}
