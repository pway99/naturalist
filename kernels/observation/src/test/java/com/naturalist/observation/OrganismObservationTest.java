package com.naturalist.observation;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameReconstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrganismObservationTest {

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
        var obs = new OrganismObservation(
                OrganismObservationId.create(), NaturalistName.of("ada"),
                new FakeGenusName("salvia"), Instant.parse("2026-08-19T00:00:00Z"),
                null, null, null);
        var updated = obs.withNotes("in the herb bed");
        assertThat(updated.notes()).isEqualTo("in the herb bed");
        assertThat(updated.subject()).isEqualTo(obs.subject());
        assertThat(updated.id()).isEqualTo(obs.id());
    }

    @Test
    void serializesSubjectAsSelfDescribingObjectAndRoundtrips() throws Exception {
        var obs = new OrganismObservation(
                OrganismObservationId.create(), NaturalistName.of("ada"),
                new FakeGenusName("salvia"), Instant.parse("2026-08-19T00:00:00Z"),
                null, null, null);
        String json = mapper().writeValueAsString(obs);
        assertThat(json).contains("\"rank\":\"GENUS\"").contains("\"value\":\"salvia\"");
        var decoded = mapper().readValue(json, OrganismObservation.class);
        assertThat(decoded.subject().value()).isEqualTo("salvia");
    }
}
