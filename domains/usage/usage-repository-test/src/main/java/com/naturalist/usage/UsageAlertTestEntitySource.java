package com.naturalist.usage;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class UsageAlertTestEntitySource extends TestEntitySource<UsageAlertId, UsageAlert> {

    public UsageAlertTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("usage/usage-alerts.json");
    }

    /** Dedup key: one alert per (counter, scope, kind, period). */
    @Override
    protected List<UniqueConstraint<UsageAlert>> uniqueConstraints() {
        return List.of(new UniqueConstraint<>() {
            @Override
            public String name() {
                return "counter+scope+kind+period";
            }

            @Override
            public Function<UsageAlert, ?> valueFunction() {
                return alert -> alert.counter().value()
                        + ":" + alert.scope()
                        + ":" + alert.kind()
                        + ":" + alert.period();
            }
        });
    }

    @Override
    protected List<ForeignKeyConstraint<UsageAlert, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "counter",
                UsageAlert::counter,
                UsageCounterTestEntitySource.class));
    }
}
