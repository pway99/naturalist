package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code usage/usage-tallies.json} seeds no rows — tallies accrue at runtime — so
 * this does not extend the generic {@code TestEntitySourceTest} (its
 * {@code dataLoads()}/{@code hasAtLeastMinimumEntities()} checks require a non-empty
 * catalog). Coverage instead round-trips an inserted entity, mirroring
 * {@code TestEntitySourceSaveTest}'s use of {@link NaturalistTestExtension}.
 */
class UsageTallyTestEntitySourceTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void roundTripInsertIsRetrievable() {
        UsageTallyTestEntitySource source = nte.getNamed(UsageTallyTestEntitySource.class);
        UsageTally tally = new UsageTally(
                UsageTallyId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                NaturalistName.of("patrick-way"),
                "daily-2026-08-25",
                1);

        source.insert(tally);

        assertThat(source.getByName(tally.id())).contains(tally);
    }

    @Test
    void roundTripInsertOfGlobalTallyIsRetrievable() {
        UsageTallyTestEntitySource source = nte.getNamed(UsageTallyTestEntitySource.class);
        UsageTally tally = new UsageTally(
                UsageTallyId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                null,
                "rate-2026-08-25-14-30",
                3);

        source.insert(tally);

        assertThat(source.getByName(tally.id())).contains(tally);
    }
}
