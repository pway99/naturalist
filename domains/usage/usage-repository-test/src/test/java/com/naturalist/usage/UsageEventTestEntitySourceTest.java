package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code usage/usage-events.json} seeds no rows — events are appended at runtime — so
 * this does not extend the generic {@code TestEntitySourceTest} (its
 * {@code dataLoads()}/{@code hasAtLeastMinimumEntities()} checks require a non-empty
 * catalog). Coverage instead round-trips an inserted entity, using
 * {@link NaturalistTestExtension} directly.
 */
class UsageEventTestEntitySourceTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void roundTripInsertIsRetrievable() {
        UsageEventTestEntitySource source = nte.getNamed(UsageEventTestEntitySource.class);
        UsageEvent event = new UsageEvent(
                UsageEventId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                NaturalistName.of("patrick-way"),
                Instant.parse("2026-08-25T10:00:00Z"));

        source.insert(event);

        assertThat(source.getByName(event.id())).contains(event);
    }
}
