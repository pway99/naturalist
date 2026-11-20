package com.naturalist.spring;

import com.naturalist.ddd.EntityId;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedValue;
import com.naturalist.ddd.NumericNamedValue;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.support.DefaultConversionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A path variable or request parameter reads through the same strategy as JSON does. */
class NamedValueConversionTest {

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

    record Label(String value) implements NamedValue<String> {
        public static Label of(String value) {
            return new Label(value);
        }

        @Override
        public boolean isValid() {
            return value != null && !value.isBlank();
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

    private static DefaultConversionService registered() {
        DefaultConversionService service = new DefaultConversionService();
        NamedValueConversion.register(service);
        return service;
    }

    @Test
    void slug_convertsThroughTheStrategy() {
        assertThat(registered().convert("Not A Slug", SlugName.class).value()).isEqualTo("Not A Slug");
    }

    @Test
    void malformedId_isNull_notAConversionFailure() {
        assertThatThrownBy(() -> new DefaultConversionService().convert("nope", ThingId.class))
                .isInstanceOf(ConversionException.class);

        assertThat(registered().convert("nope", ThingId.class)).isNull();
    }

    @Test
    void numeric_converts() {
        assertThat(registered().convert("6.50", Grams.class).value()).isEqualTo(new BigDecimal("6.50"));
    }

    @Test
    void recordNamedValue_converts() {
        assertThat(registered().convert("a.jpg", Label.class).value()).isEqualTo("a.jpg");
    }
}
