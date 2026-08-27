package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

/**
 * No {@code foreignKeyConstraints()} override: {@link UsageEvent#counterName()} names
 * a counter *activity*, not a single {@link UsageCounter} row — {@code UsageCounter}
 * is now keyed by {@link UsageCounterId}, and several rules may share one
 * {@code counterName} (see {@code usage/usage-counters.json}), so it is no longer a
 * single-row FK target.
 */
public class UsageEventTestEntitySource extends TestEntitySource<UsageEventId, UsageEvent> {

    public UsageEventTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("usage/usage-events.json");
    }
}
