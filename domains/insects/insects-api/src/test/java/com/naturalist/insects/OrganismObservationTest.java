package com.naturalist.insects;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import com.naturalist.observation.OrganismObservation;

class OrganismObservationTest {

    private final OrganismObservation<InsectObservationId, InsectRankName> base = new OrganismObservation<InsectObservationId, InsectRankName>(
            InsectObservationId.create(),
            NaturalistName.of("pat"),
            InsectSpeciesName.of("battus-philenor"),
            Instant.parse("2026-07-01T12:00:00Z"),
            "original notes",
            "oak vista",
            null);

    @Test
    void withNotes_replacesNotes() {
        var updated = base.withNotes("new notes");
        assertThat(updated.notes()).isEqualTo("new notes");
        assertThat(updated.id()).isEqualTo(base.id());
        assertThat(updated.observedBy()).isEqualTo(base.observedBy());
        assertThat(updated.subject()).isEqualTo(base.subject());
        assertThat(updated.observedOn()).isEqualTo(base.observedOn());
        assertThat(updated.location()).isEqualTo(base.location());
        assertThat(updated.identification()).isEqualTo(base.identification());
    }

    @Test
    void withNotes_acceptsNull() {
        var updated = base.withNotes(null);
        assertThat(updated.notes()).isNull();
    }

    @Test
    void withSubject_replacesSubject() {
        var newSubject = InsectGenusName.of("battus");
        var updated = base.withSubject(newSubject);
        assertThat(updated.subject()).isEqualTo(newSubject);
        assertThat(updated.id()).isEqualTo(base.id());
        assertThat(updated.observedBy()).isEqualTo(base.observedBy());
        assertThat(updated.notes()).isEqualTo(base.notes());
        assertThat(updated.observedOn()).isEqualTo(base.observedOn());
        assertThat(updated.location()).isEqualTo(base.location());
        assertThat(updated.identification()).isEqualTo(base.identification());
    }
}
