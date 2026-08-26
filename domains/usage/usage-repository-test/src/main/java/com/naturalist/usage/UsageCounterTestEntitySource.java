package com.naturalist.usage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class UsageCounterTestEntitySource extends TestEntitySource<UsageCounterName, UsageCounter> {

    public UsageCounterTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("usage/usage-counters.json");
    }
}
