package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code usage/usage-alerts.json} seeds no rows — alerts are raised at runtime — so
 * this does not extend the generic {@code TestEntitySourceTest} (its
 * {@code dataLoads()}/{@code hasAtLeastMinimumEntities()} checks require a non-empty
 * catalog). Coverage instead round-trips an inserted entity, mirroring
 * {@code TestEntitySourceSaveTest}'s use of {@link NaturalistTestExtension}.
 */
class UsageAlertTestEntitySourceTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void roundTripInsertIsRetrievable() {
        UsageAlertTestEntitySource source = nte.getNamed(UsageAlertTestEntitySource.class);
        UsageAlert alert = new UsageAlert(
                UsageAlertId.create(),
                TestUsageIdentifiers.UsageCounters.Identification,
                AlertScope.DAILY,
                AlertKind.WARNING,
                "daily-2026-08-25",
                "80% of daily identification budget used",
                Instant.parse("2026-08-25T14:30:00Z"),
                false,
                false);

        source.insert(alert);

        assertThat(source.getByName(alert.id())).contains(alert);
    }
}
