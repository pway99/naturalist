package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.TestEntitySourceTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class UsageCounterTestEntitySourceTest
        extends TestEntitySourceTest<UsageCounterName, UsageCounter, UsageCounterTestEntitySource> {

    /**
     * Only one real counter is seeded so far ({@code identification}) — see
     * {@code usage/usage-counters.json}. Mirrors the Oak Vista override precedent
     * in {@link TestEntitySourceTest#minimumEntities()}.
     */
    @Override
    protected int minimumEntities() {
        return 1;
    }

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void seededIdentificationCounterLoads() {
        UsageCounterTestEntitySource source = nte.getNamed(UsageCounterTestEntitySource.class);

        assertThat(source.getByName(TestUsageIdentifiers.UsageCounters.Identification))
                .contains(new UsageCounter(TestUsageIdentifiers.UsageCounters.Identification));
    }

    @Test
    void insertedCounterIsRetrievable() {
        UsageCounterTestEntitySource source = nte.getNamed(UsageCounterTestEntitySource.class);
        UsageCounter counter = new UsageCounter(TestUsageIdentifiers.UsageCounters.InsectIdentification);

        source.insert(counter);

        assertThat(source.getByName(TestUsageIdentifiers.UsageCounters.InsectIdentification))
                .contains(counter);
    }
}
