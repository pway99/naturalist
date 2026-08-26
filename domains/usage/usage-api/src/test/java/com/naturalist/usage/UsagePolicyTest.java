package com.naturalist.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UsagePolicyTest {

    @Test
    void warning_threshold_rounds_up() {
        assertThat(UsagePolicy.warningThreshold(650, 80)).isEqualTo(520);
        assertThat(UsagePolicy.warningThreshold(50, 80)).isEqualTo(40);
        assertThat(UsagePolicy.warningThreshold(3, 80)).isEqualTo(3);
    }

    @Test
    void message_names_scope_and_numbers() {
        var m = UsagePolicy.alertMessage(AlertScope.MONTHLY, AlertKind.HARD_STOP, 520, 650);
        assertThat(m).contains("monthly").contains("520").contains("650");
    }

    @Test
    void scopeOf_maps_and_rejects() {
        assertThat(UsagePolicy.scopeOf(LimitKind.DAILY)).isEqualTo(AlertScope.DAILY);
        assertThat(UsagePolicy.scopeOf(LimitKind.MONTHLY)).isEqualTo(AlertScope.MONTHLY);
        assertThrows(IllegalArgumentException.class, () -> UsagePolicy.scopeOf(LimitKind.PER_USER));
        assertThrows(IllegalArgumentException.class, () -> UsagePolicy.scopeOf(LimitKind.RATE));
    }
}
