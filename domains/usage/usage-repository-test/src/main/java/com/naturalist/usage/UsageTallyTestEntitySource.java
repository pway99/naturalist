package com.naturalist.usage;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class UsageTallyTestEntitySource extends TestEntitySource<UsageTallyId, UsageTally> {

    public UsageTallyTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("usage/usage-tallies.json");
    }

    /**
     * Business key: one tally per (counter, naturalist, period). {@code naturalist}
     * is null for a global tally, so it is folded into the composite string with an
     * explicit sentinel rather than left out — two global tallies for the same
     * counter+period would otherwise collide with two distinct per-user tallies
     * whose {@code naturalist} happens to differ only by being absent.
     */
    @Override
    protected List<UniqueConstraint<UsageTally>> uniqueConstraints() {
        return List.of(new UniqueConstraint<>() {
            @Override
            public String name() {
                return "counter+naturalist+period";
            }

            @Override
            public Function<UsageTally, ?> valueFunction() {
                return tally -> tally.counter().value()
                        + ":" + (tally.naturalist() == null ? "" : tally.naturalist().value())
                        + ":" + tally.period();
            }
        });
    }

    @Override
    protected List<ForeignKeyConstraint<UsageTally, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "counter",
                UsageTally::counter,
                UsageCounterTestEntitySource.class));
    }
}
