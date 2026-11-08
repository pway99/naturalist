package com.naturalist.usage;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.test.context.Fake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The entitled branch of {@link UsageCommandImpl#reserve}. Production's {@link NoOpEntitlementLookup}
 * entitles nobody, so the test chooses who is entitled through {@link UsagesTestContextInternal} before
 * the command is built; that is why the command comes from {@code nte.get} here and not a
 * {@code @Wired} field, which would be built before the test runs.
 */
class UsageCommandEntitlementTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Fake
    Clock clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void entitled_naturalist_bypasses_public_rules() {
        NaturalistName entitled = NaturalistName.of("entitled-naturalist");
        nte.context(UsagesTestContextInternal.class).entitlements(entitled::equals);
        UsageRepository.CounterRepository counters = nte.get(UsageRepository.CounterRepository.class);
        UsageCounter dailyCap = counters.findByCounterName(UsageCounterName.of("identification")).stream()
                .filter(c -> c.scope() == UsageScope.GLOBAL && c.windowKind() == WindowKind.CALENDAR_DAY)
                .findFirst()
                .orElseThrow();
        counters.save(dailyCap.withLimit(0));
        UsageCommand command = nte.get(UsageCommand.class);

        command.reserve(entitled);

        assertThat(nte.get(UsageRepository.EventRepository.class).findByCounterSince(
                UsageCounterName.of("identification"), entitled, Instant.EPOCH)).hasSize(1);
        assertThatThrownBy(() -> command.reserve(NaturalistName.of("public-naturalist")))
                .as("the same zero cap still binds a naturalist who is not entitled")
                .isInstanceOf(BudgetExceededException.class);
    }
}
