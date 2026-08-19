package com.naturalist.plants;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismObservation;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlantObservationTest {

    private static final Observer observer = Observer.forClass(PlantObservationTest.class);

    @Test
    void fullyPopulatedObservationIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedObservationIsValid");
        OrganismObservation<PlantObservationId, PlantRankName> observation =
                new OrganismObservation<>(
                        PlantObservationId.create(),
                        NaturalistName.of("patrick-way"),
                        PlantSpeciesName.of("solanum-lycopersicum"),
                        Instant.parse("2026-08-18T15:00:00Z"),
                        "first fruit set",
                        "south bed",
                        null);

        InvariantObservation result = mo.namedEntity(observation, "observation");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullNotesAndLocationAreValid() {
        // notes and location are nullable-by-design — an observation can be a bare sighting.
        MethodObserver mo = observer.forMethod("nullNotesAndLocationAreValid");
        OrganismObservation<PlantObservationId, PlantRankName> observation =
                new OrganismObservation<>(
                        PlantObservationId.create(),
                        NaturalistName.of("patrick-way"),
                        PlantFamilyName.of("lamiaceae"),
                        Instant.parse("2026-08-18T15:00:00Z"),
                        null,
                        null,
                        null);

        InvariantObservation result = mo.namedEntity(observation, "observation");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        OrganismObservation<PlantObservationId, PlantRankName> observation =
                new OrganismObservation<>(null, null, null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(observation, "observation");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".observation.id",
                        ".observation.observedBy",
                        ".observation.subject",
                        ".observation.observedOn");
    }

    @Test
    void withNotesReturnsNewInstanceLeavingOriginalUnchanged() {
        OrganismObservation<PlantObservationId, PlantRankName> observation =
                new OrganismObservation<>(
                        PlantObservationId.create(),
                        NaturalistName.of("patrick-way"),
                        PlantGenusName.of("trifolium"),
                        Instant.parse("2026-08-18T15:00:00Z"),
                        null,
                        null,
                        null);

        OrganismObservation<PlantObservationId, PlantRankName> updated = observation.withNotes("flowering heavily");

        assertThat(updated.notes()).isEqualTo("flowering heavily");
        assertThat(observation.notes()).isNull();
    }
}
