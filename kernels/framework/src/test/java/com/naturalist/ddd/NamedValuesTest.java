package com.naturalist.ddd;

import com.naturalist.exception.InvariantViolationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The one strategy every named value is read through: built as given, unreadable text is null. */
class NamedValuesTest {

    static final class SlugName extends EntityName {
        private SlugName(String value) {
            super(value);
        }

        public static SlugName of(String value) {
            return new SlugName(value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }

    record Grams(BigDecimal value) implements NumericNamedValue {
        public static Grams of(BigDecimal value) {
            return new Grams(value);
        }

        @Override
        public boolean isValid() {
            return value != null;
        }

        @Override
        public int scale() {
            return 2;
        }

        @Override
        public RoundingMode roundingMode() {
            return RoundingMode.HALF_UP;
        }
    }

    static final class ThingId extends EntityId {
        private ThingId(UUID value) {
            super(value);
        }

        public static ThingId of(UUID value) {
            return new ThingId(value);
        }
    }

    /** Two qualifying factories: which one would run is not knowable, so the type is refused. */
    static final class TwoWays extends EntityName {
        private TwoWays(String value) {
            super(value);
        }

        public static TwoWays of(String value) {
            return new TwoWays(value);
        }

        public static TwoWays of(UUID value) {
            return new TwoWays(value.toString());
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }

    static final class Brittle extends EntityName {
        private Brittle(String value) {
            super(value);
        }

        public static Brittle of(String value) {
            throw new IllegalArgumentException("refused: " + value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @BeforeEach
    void bindMeters() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void unbindMeters() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void read_string_buildsEvenWhenInvalid() {
        SlugName name = NamedValues.read(SlugName.class, "Not A Slug");

        assertThat(name).isNotNull();
        assertThat(name.value()).isEqualTo("Not A Slug");
        assertThat(name.isValid()).isFalse();
    }

    @Test
    void read_decimal_keepsScale() {
        assertThat(NamedValues.read(Grams.class, "6.50").value()).isEqualTo(new BigDecimal("6.50"));
    }

    @Test
    void read_decimal_unreadable_isNull() {
        assertThat(NamedValues.read(Grams.class, "six")).isNull();
        assertThat(NamedValues.read(Grams.class, "")).isNull();
    }

    @Test
    void read_uuid_nonV7_isHandedOn() {
        ThingId id = NamedValues.read(ThingId.class, "123e4567-e89b-42d3-a456-426614174000");

        assertThat(id).isNotNull();
        assertThat(id.isValid()).isFalse();
    }

    @Test
    void read_uuid_malformed_isNull() {
        assertThat(NamedValues.read(ThingId.class, "not-a-uuid")).isNull();
    }

    @Test
    void read_null_isNull() {
        assertThat(NamedValues.read(SlugName.class, null)).isNull();
    }

    @Test
    void read_noFactory_refusedAndCounted() {
        assertThatThrownBy(() -> NamedValues.read(EntityName.class, "x"))
                .isInstanceOf(InvariantViolationException.class);

        Counter refused = meters.find("naturalist.invariant.check")
                .tag("name", "NamedValues.read.factoryFound")
                .tag("outcome", "violated")
                .counter();
        assertThat(refused).isNotNull();
        assertThat(refused.count()).isEqualTo(1.0);
    }

    @Test
    void read_twoFactories_refusedAndCounted() {
        assertThatThrownBy(() -> NamedValues.read(TwoWays.class, "x"))
                .isInstanceOf(InvariantViolationException.class);

        Counter refused = meters.find("naturalist.invariant.check")
                .tag("name", "NamedValues.read.factoryUnique")
                .tag("outcome", "violated")
                .counter();
        assertThat(refused).isNotNull();
        assertThat(refused.count()).isEqualTo(1.0);
    }

    @Test
    void read_factoryThrows_countsTheFactorysOwnException() {
        assertThatThrownBy(() -> NamedValues.read(Brittle.class, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);

        Counter built = meters.find("naturalist.invariant.check")
                .tag("name", "NamedValues.read.built")
                .tag("exception", "IllegalArgumentException")
                .counter();
        assertThat(built).isNotNull();
        assertThat(built.count()).isEqualTo(1.0);
    }

    @Test
    void read_decimal_hugeExponent_isNull() {
        assertThat(NamedValues.read(Grams.class, "1e99999")).isNull();
        assertThat(NamedValues.read(Grams.class, "1.5e3").value()).isEqualTo(new BigDecimal("1.5e3"));
    }
}
