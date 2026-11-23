package com.naturalist.ddd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.naturalist.exception.InvariantViolationException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The supertypes own deserialization: every subtype reads through {@link NamedValues}, nested or not. */
class NamedValueDeserializerTest {

    private static final String V7 = "019a8f3e-2b71-7a01-8b01-000000000001";

    private final ObjectMapper mapper = new ObjectMapper();

    static final class SlugName extends EntityName {
        private SlugName(String value) {
            super(value);
        }

        public static SlugName of(String value) {
            return new SlugName(value);
        }

        /** What Jackson would use if the subtype still owned deserialization. */
        @JsonCreator
        static SlugName fromJson(String value) {
            return new SlugName("creator:" + value);
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

    record Holder(SlugName slug, Label label, Grams grams, ThingId id,
                  List<SlugName> slugs, Map<String, SlugName> bySlot) {
    }

    record Keyed(Map<SlugName, Integer> bySlug, Map<ThingId, Integer> byId, Map<Grams, Integer> byGrams) {
    }

    @Test
    void entityName_supertypeWinsOverSubtypeCreator() throws Exception {
        assertThat(mapper.readValue("\"bee\"", SlugName.class).value()).isEqualTo("bee");
    }

    @Test
    void namedValue_interfaceAnnotationApplies() throws Exception {
        assertThat(mapper.readValue("\"x\"", Label.class).value()).isEqualTo("x");
    }

    @Test
    void numeric_fromNumberToken_keepsScale() throws Exception {
        assertThat(mapper.readValue("6.50", Grams.class).value()).isEqualTo(new BigDecimal("6.50"));
    }

    @Test
    void numeric_fromStringToken() throws Exception {
        assertThat(mapper.readValue("\"6.5\"", Grams.class).value()).isEqualTo(new BigDecimal("6.5"));
    }

    @Test
    void entityId_malformed_isNull() throws Exception {
        assertThat(mapper.readValue("\"nope\"", ThingId.class)).isNull();
    }

    @Test
    void nested_fieldListAndMapValue() throws Exception {
        Holder holder = mapper.readValue("""
                {"slug":"a","label":"b","grams":1.25,"id":"%s","slugs":["c"],"bySlot":{"k":"d"}}
                """.formatted(V7), Holder.class);

        assertThat(holder.slug().value()).isEqualTo("a");
        assertThat(holder.label().value()).isEqualTo("b");
        assertThat(holder.grams().value()).isEqualTo(new BigDecimal("1.25"));
        assertThat(holder.id().value()).isEqualTo(UUID.fromString(V7));
        assertThat(holder.slugs()).extracting(SlugName::value).containsExactly("c");
        assertThat(holder.bySlot().get("k").value()).isEqualTo("d");
    }

    @Test
    void nonScalarToken_isRefusedAndCounted() {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
        try {
            assertThatThrownBy(() -> mapper.readValue("{\"value\":\"a\"}", SlugName.class))
                    .isInstanceOfAny(InvariantViolationException.class, JsonMappingException.class);

            Counter refused = meters.find("naturalist.invariant.check")
                    .tag("name", "NamedValueDeserializer.deserialize.scalarToken")
                    .tag("outcome", "violated")
                    .counter();
            assertThat(refused).isNotNull();
            assertThat(refused.count()).isEqualTo(1.0);
        } finally {
            Metrics.removeRegistry(meters);
        }
    }

    @Test
    void serialization_unchanged() throws Exception {
        Holder holder = new Holder(SlugName.of("a"), Label.of("b"), Grams.of(new BigDecimal("1.25")),
                ThingId.of(UUID.fromString(V7)), List.of(SlugName.of("c")), Map.of("k", SlugName.of("d")));

        assertThat(mapper.writeValueAsString(holder)).isEqualTo("""
                {"slug":"a","label":"b","grams":1.25,"id":"%s","slugs":["c"],"bySlot":{"k":"d"}}"""
                .formatted(V7));
    }

    @Test
    void mapKeys_readThroughTheStrategy() throws Exception {
        Keyed keyed = mapper.readValue("""
                {"bySlug":{"bee":1},"byId":{"%s":2},"byGrams":{"6.50":3}}
                """.formatted(V7), Keyed.class);

        assertThat(keyed.bySlug()).containsEntry(SlugName.of("bee"), 1);
        assertThat(keyed.byId()).containsEntry(ThingId.of(UUID.fromString(V7)), 2);
        assertThat(keyed.byGrams()).containsEntry(Grams.of(new BigDecimal("6.50")), 3);
    }

    @Test
    void mapKey_atTheRoot() throws Exception {
        Map<ThingId, Integer> byId = mapper.readValue("{\"%s\":1}".formatted(V7),
                new com.fasterxml.jackson.core.type.TypeReference<Map<ThingId, Integer>>() {
                });

        assertThat(byId).containsEntry(ThingId.of(UUID.fromString(V7)), 1);
    }

    @Test
    void mapKey_invalidSlug_isHandedOn_unreadableId_isNull() throws Exception {
        Keyed keyed = mapper.readValue("""
                {"bySlug":{"Not A Slug":1},"byId":{"nope":2},"byGrams":{}}
                """, Keyed.class);

        assertThat(keyed.bySlug().keySet()).extracting(SlugName::value).containsExactly("Not A Slug");
        assertThat(keyed.byId()).containsEntry(null, 2);
    }

    @Test
    void mapKeys_serializeAsTheirValue() throws Exception {
        Keyed keyed = new Keyed(Map.of(SlugName.of("bee"), 1), Map.of(ThingId.of(UUID.fromString(V7)), 2),
                Map.of(Grams.of(new BigDecimal("6.50")), 3));

        assertThat(mapper.writeValueAsString(keyed)).isEqualTo("""
                {"bySlug":{"bee":1},"byId":{"%s":2},"byGrams":{"6.50":3}}""".formatted(V7));
    }
}
