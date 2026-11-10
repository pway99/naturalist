package com.naturalist.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.naturalist.ddd.EntityId;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameReconstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganismObservationTest {

    private static final Observer OBSERVER = Observer.forClass(OrganismObservationTest.class);

    static final class FakeObsId extends EntityId {
        private FakeObsId(UUID v) { super(v); }
        @JsonCreator static FakeObsId of(UUID v) { return new FakeObsId(v); }
        static FakeObsId create() { return new FakeObsId(EntityId.newUUID()); }
    }

    record FakeGenusName(String value) implements RankName {
        @Override public LinealRank rank() { return LinealRank.GENUS; }
    }

    private static final RankNameReconstructor RECONSTRUCTOR =
            (slug, rank) -> new FakeGenusName(slug);

    private static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setInjectableValues(new InjectableValues.Std()
                        .addValue(RankNameReconstructor.class, RECONSTRUCTOR));
    }

    @Test
    void withNotesPreservesEverythingElse() {
        OrganismObservation<FakeObsId, FakeGenusName> obs = new OrganismObservation<>(
                FakeObsId.create(), NaturalistName.of("ada"),
                new FakeGenusName("salvia"), Instant.parse("2026-08-19T00:00:00Z"),
                null, null, null, null);
        var updated = obs.withNotes("in the herb bed");
        assertThat(updated.notes()).isEqualTo("in the herb bed");
        assertThat(updated.subject()).isEqualTo(obs.subject());
        assertThat(updated.id()).isEqualTo(obs.id());
    }

    @Test
    void serializesSubjectAsSelfDescribingObjectAndRoundtrips() throws Exception {
        OrganismObservation<FakeObsId, FakeGenusName> obs = new OrganismObservation<>(
                FakeObsId.create(), NaturalistName.of("ada"),
                new FakeGenusName("salvia"), Instant.parse("2026-08-19T00:00:00Z"),
                null, null, null, null);
        String json = mapper().writeValueAsString(obs);
        assertThat(json).contains("\"rank\":\"GENUS\"").contains("\"value\":\"salvia\"");
        var type = mapper().getTypeFactory().constructParametricType(OrganismObservation.class, FakeObsId.class, FakeGenusName.class);
        OrganismObservation<FakeObsId, FakeGenusName> decoded = mapper().readValue(json, type);
        assertThat(decoded.subject().value()).isEqualTo("salvia");
        assertThat(decoded.id()).isEqualTo(obs.id());
    }

    private static OrganismObservation<FakeObsId, FakeGenusName> observationIn(GridCell area) {
        return new OrganismObservation<>(
                FakeObsId.create(), NaturalistName.of("ada"),
                new FakeGenusName("salvia"), Instant.parse("2026-08-19T00:00:00Z"),
                null, null, area, null);
    }

    @Test
    void area_isOptional() {
        var mo = OBSERVER.forMethod("area_isOptional");

        var result = mo.namedEntity(observationIn(null), "observation");

        // the fake subject is not a real identifier, so the only thing proved here is that a null area is no violation
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .doesNotContain(".observation.area");
    }

    @Test
    void area_roundTripsAndWithAreaReplaces() throws Exception {
        var obs = observationIn(GridCell.of(1092, -2945));
        String json = mapper().writeValueAsString(obs);
        assertThat(json).contains("\"area\":{\"latIndex\":1092,\"lonIndex\":-2945}");
        var type = mapper().getTypeFactory().constructParametricType(OrganismObservation.class, FakeObsId.class, FakeGenusName.class);
        OrganismObservation<FakeObsId, FakeGenusName> decoded = mapper().readValue(json, type);
        assertThat(decoded.area()).isEqualTo(GridCell.of(1092, -2945));

        var moved = obs.withArea(GridCell.of(1, 2));
        assertThat(moved.area()).isEqualTo(GridCell.of(1, 2));
        assertThat(moved.withArea(null).area()).isNull();
        assertThat(moved.id()).isEqualTo(obs.id());
        assertThat(obs.withNotes("x").area()).isEqualTo(obs.area());
        assertThat(obs.withSubject(new FakeGenusName("mentha")).area()).isEqualTo(obs.area());
    }
}
